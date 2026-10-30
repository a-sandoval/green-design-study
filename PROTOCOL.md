# Protocolo Experimental — Green Design Study

## Pregunta de investigación

¿La decisión arquitectónica entre monolito modular y microservicios (database-per-service) condiciona el consumo energético de un sistema CRUD e-commerce?

## Hipótesis

H₁: El brazo microservicios consume más energía que el monolito para el mismo workload, debido al overhead de composición por red (HTTP) vs composición en proceso (SQL JOIN).

H₀: No hay diferencia significativa en el consumo energético entre ambos brazos.

## Variable independiente

| Factor | Niveles |
|---|---|
| Arquitectura | Monolito modular, Microservicios (database-per-service) |
| Ratio lectura:escritura | 80:20, 50:50, 20:80 |

Diseño factorial 2 × 3. Cada celda: 30 repeticiones.

## Variables dependientes

- Energía consumida (kWh) — CodeCarbon v3 en modo `machine` (equipo completo), CPU vía Windows EMI/RAPL; ver "Configuración efectiva de la medición"
- Emisiones de CO₂ (kg) — calculadas por CodeCarbon con su propio factor de intensidad (efectivo: 354,103 gCO₂e/kWh, constante en las 179 filas); ver "Factor de carbono y SCI"
- Latencia (ms) — k6 reporta mínimo, promedio, mediana, p90, p95 y máximo; **no reporta p99** (ausente en los 179 `k6_stdout`). El resumen de k6 se conserva como texto en la columna `k6_stdout` de cada CSV y `analysis/analyze.py` no lo procesa: las latencias no se analizaron estadísticamente en esta campaña.

## Variables controladas

| Variable | Valor | Justificación |
|---|---|---|
| Hardware | AMD Ryzen 7 7735HS, 32 GB RAM (30,78 GB utilizables) | Mismo host para ambos brazos |
| JDK | Eclipse Temurin 21 | Misma JVM |
| Framework | Spring Boot 3.3.5 | Mismo framework y versión |
| Base de datos | PostgreSQL 16 | Misma versión y config |
| Lógica de negocio | Módulo `domain` compartido | Elimina variación en validación |
| Recursos Docker (app) | Monolito: 1×(3 CPU + 768M); Micros: 3×(1 CPU + 256M) = 3 CPU + 768M | Simétrico entre brazos |
| Recursos Docker (BD) | 1 instancia PostgreSQL en ambos brazos: 2 CPU + 512M | En micros, misma instancia con 3 schemas separados |
| Presupuesto total por brazo | 5 CPU + 1280 MB | App + BD idéntico en ambos brazos |
| Pool de conexiones DB | Monolito: 20; Micros: 7 × 3 = 21 | Normalizado |
| Datos seed | 20 clientes + 30 productos | Idénticos en ambos brazos |
| PostgreSQL config | autovacuum=off, logging=off durante medición | Idéntica en ambos brazos, reduce ruido |
| Semilla RNG del workload | `42 + rep_number` | Reproducibilidad |

## Protocolo de ejecución

### Pre-medición

1. Cerrar todas las aplicaciones no esenciales del host
2. Desactivar actualizaciones de Windows, antivirus real-time si es posible
3. Conectar laptop a corriente (no batería)
4. Esperar 5 min de reposo antes de comenzar
5. Verificar que DVFS/turbo boost esté en estado consistente (perfil "Equilibrado")

Cumplimiento de la pre-medición en la campaña ejecutada: el perfil de energía "Equilibrado" y la suspensión/hibernación desactivadas con corriente alterna se verificaron con `powercfg` en la sesión de trabajo del 2026-09-12, y las aplicaciones no esenciales se cerraron antes del lanzamiento; ninguna de estas verificaciones quedó registrada en los datos de la campaña. Según el responsable de la campaña, Windows Update y el antivirus (Windows Defender) estaban desactivados durante la ejecución; ese estado no quedó registrado en los datos. No se registró si se respetó la espera de 5 min.

### Por cada repetición

1. `docker compose up -d --build` — levantar el brazo
2. Health check — esperar que todos los servicios respondan 200
3. Warm-up: 120s de carga con k6 (5 VUs) — estabilizar JIT y pools. Siempre con ratio 80:20 y semilla 1, también en las celdas 50:50 y 20:80 (`harness/run_experiment.py:105-106`). Queda fuera de la ventana de medición
4. Iniciar CodeCarbon EmissionsTracker
5. Ejecutar k6: 60s, 10 VUs, ratio configurado, semilla fija
6. Detener CodeCarbon — registrar energía y emisiones
7. `docker compose down -v` — bajar completamente
8. Cooldown: 60s de reposo — disipar calor, normalizar CPU

### Configuración efectiva de la medición

Verificada en `harness/run_experiment.py:180-193` y por instanciación de `EmissionsTracker` con codecarbon 3.3.1:

- El harness crea `EmissionsTracker(project_name=run_id, output_dir=..., log_level="warning", save_to_file=False)`. No se fijan `measure_power_secs` ni `tracking_mode`, por lo que rigen los valores por defecto: **intervalo de muestreo de 15 s** (no 1 s) y **modo `machine`** (consumo del equipo completo, no del proceso).
- Con `save_to_file=False` sólo se conservan `energy_kwh` (`tracker._total_energy.kWh`) y `emissions_kg_co2` (retorno de `tracker.stop()`). El desglose CPU/RAM no quedó registrado para la campaña.
- En una prueba previa de CodeCarbon (`emissions.csv`, 2026-08-23, codecarbon 3.3.0, fuera del repositorio) el instrumento reportó la CPU vía Windows EMI y la RAM con una potencia constante de 20,0 W, es decir, estimada por modelo y no medida.
- Versiones: CodeCarbon 3.3.x (versión exacta no registrada; la prueba de agosto usó 3.3.0). Python: versión exacta no registrada; la prueba de agosto usó 3.12.10.

### Generador de carga dentro de la ventana de medición

k6 (binario nativo de Windows) corre en el mismo equipo que los contenedores y apunta a `localhost`. Se ejecuta entre `tracker.start()` y `tracker.stop()` (`harness/run_experiment.py:186-193`), por lo que su consumo, el de Docker y el de cualquier otra actividad del host quedan integrados en la energía medida (modo `machine`). El warm-up (120 s) se ejecuta antes de `tracker.start()` y no integra la medición.

### Orden de ejecución

**Planificado (no ejecutado así):** alternar los brazos dentro de cada ratio (Monolito 80:20, Microservicios 80:20, Monolito 50:50, y así sucesivamente) para mitigar los efectos de orden térmico.

**Ejecutado:** por bloques de brazo, tal como lo define `harness/run_all.py:21-31` (lazo externo = brazo; lazo interno = ratio 80, 50, 20). Inicio de cada celda según el timestamp del `run_id` de su CSV (hora local, UTC−3):

| # | Celda | Inicio | CSV |
|---|---|---|---|
| 1 | Monolito 80:20 | 2026-09-12 22:53 | `monolith_r80_20260912_225348.csv` |
| 2 | Monolito 50:50 | 2026-09-13 00:57 | `monolith_r50_20260913_005751.csv` |
| 3 | Monolito 20:80 | 2026-09-13 03:03 | `monolith_r20_20260913_030303.csv` |
| 4 | Microservicios 80:20 | 2026-09-13 05:08 | `microservices_r80_20260913_050815.csv` |
| 5 | Microservicios 50:50 | 2026-09-13 07:16 | `microservices_r50_20260913_071641.csv` |
| 6 | Microservicios 20:80 | 2026-09-13 09:25 | `microservices_r20_20260913_092501.csv` |

La última escritura del último CSV es de las 11:33 (fecha de modificación del archivo): la campaña abarcó unas 12 h 40 min.

El agrupamiento por bloques confunde la arquitectura con la evolución del entorno: los tres bloques de monolito precedieron a los de microservicios, de modo que cualquier deriva del host durante la campaña (térmica, tareas de fondo, sincronización de OneDrive, hora de la noche) queda atribuida a la arquitectura. La ejecución fue secuencial (no paralela), pero no alternada.

## Análisis estadístico

### Tests primarios

- **Mann-Whitney U** — comparación no paramétrica entre brazos por ratio
- **Cliff's δ** — tamaño de efecto ordinal (pequeño ≥ 0.147, mediano ≥ 0.33, grande ≥ 0.474)
- **Vargha-Delaney Â₁₂** — probabilidad de superioridad estocástica

### Justificación de no-parametricidad

- N = 30 por celda: insuficiente para asumir normalidad
- Distribuciones de energía típicamente asimétricas
- Outliers por GC, turbo boost, actividad del host
- No se reportan media/SD; se reportan mediana + IQR

### Indicador SCI (ISO/IEC 21031:2024)

SCI = (E × I + M) / R
- E: energía medida (kWh)
- I: intensidad de carbono del grid (g CO₂/kWh) — SEN argentino 2024
- M: no estimado. Con el mismo hardware M es igual en ambos brazos pero no nulo, y como R difiere entre brazos, M/R también difiere; no se calculó el SCI completo en esta campaña
- R: número de requests completados exitosamente

## Amenazas a la validez

| Amenaza | Mitigación prevista | Estado en esta campaña |
|---|---|---|
| Thermal order effects | Cooldown 60s entre reps, intercalar brazos | Cooldown implementado entre repeticiones (`--cooldown-seconds`, 60 s por defecto); no se aplica entre celdas ni tras la última repetición de cada celda (`run_experiment.py:217-219`). **Alternancia de brazos no realizada**: ejecución por bloques (ver "Orden de ejecución"), con la arquitectura confundida con la evolución del entorno durante ~12 h 40 min. |
| DVFS / turbo boost | Perfil "Equilibrado", reportar frecuencia media | Perfil "Equilibrado" verificado con `powercfg` (2026-09-12). Reportar frecuencia media: **prevista, no implementada en esta campaña** (el harness no registra la frecuencia de CPU). |
| GC asymmetry | JDK 21 G1GC en ambos, warm-up de 120s | JDK 21 (`eclipse-temurin:21-jre-jammy`) y warm-up de 120 s implementados. **No se fijó ningún recolector y el GC efectivo fue `SerialGC`, no G1**: no hay flags de JVM (`-XX`, `JAVA_OPTS`) en los Dockerfile ni en los compose, y la JVM elige el recolector por ergonomía según los límites del contenedor. Verificado el 2026-10-30 ejecutando cada imagen local (Temurin 21.0.12+8) con los límites del compose y `-XX:+PrintFlagsFinal`: monolito (3 CPU, 768 MB) y los tres servicios (1 CPU, 256 MB cada uno) dieron `UseSerialGC=true` y `UseG1GC=false`; heap máximo de 192 MiB en el monolito y de 126 MiB en cada servicio. Los Dockerfile y los compose no cambiaron desde la campaña. El GC no se registró durante la campaña. |
| RAPL mide package, no proceso | Cerrar apps, reportar energía idle | Aplicaciones cerradas antes del lanzamiento (verificación manual, sin registro en los datos). Reportar energía idle: **prevista, no implementada en esta campaña** (no hay medición de línea base en reposo). El modo de CodeCarbon es `machine`. |
| Ruido del host | 30 repeticiones, tests no paramétricos | Implementado: 30 repeticiones por celda (29 en monolito 80:20) y Mann-Whitney U, Cliff's δ y Â₁₂ en `analysis/analyze.py`. El repositorio reside en una carpeta de OneDrive; no hay evidencia de que la sincronización se haya controlado o medido. |
| CodeCarbon EMI granularidad | Muestreo cada 1s, reportar intervalo | **Muestreo cada 15 s** (valor por defecto; el harness no configura `measure_power_secs`). El intervalo se documenta en "Configuración efectiva de la medición". |
| Sin medidor de pared | Amenaza declarada: RAPL subestima energía total | Sin cambios. |

## Factor de carbono y SCI (tal como se ejecutó)

- La columna `emissions_kg_co2` de los CSV la calcula CodeCarbon con su propio factor de intensidad. El cociente emisiones/energía es **354,103 gCO₂e/kWh y es constante en las 179 filas** (verificado sobre `results/*.csv`). CodeCarbon detectó el país como Argentina (ARG) en su registro de la prueba del 2026-08-23 (`emissions.csv`). **Las emisiones reportadas en el artículo usan este factor.**
- El valor de ~350 gCO₂/kWh (SEN 2024) de este protocolo y el parámetro `--grid-intensity` (350 por defecto) de `analysis/analyze.py` no intervienen en la columna `emissions_kg_co2`. En `analyze.py` sólo se usan en `plot_sci_bars`, que multiplica la mediana de energía por 1000 y por ese factor y no divide por R: su salida (`analysis/figures/sci_bars.png`) no es el SCI de la especificación y no debe interpretarse como tal.
- Las emisiones son una reexpresión lineal de la energía por un factor constante, no una medición independiente.
- Referencia del diseño original (no aplicada a las emisiones de la campaña): intensidad Argentina (SEN 2024) ~350 gCO₂/kWh.

## Desviaciones registradas — corrida 2026-09-12/13

- **Ejecuciones registradas: 179 de 180.** Falta la repetición 1 de monolito 80:20 (celda con N=29). `harness/run_experiment.py` escribe la fila del CSV sólo al finalizar una repetición completa; si hay una excepción, imprime el error por consola, baja los contenedores (`finally`) y continúa con la siguiente repetición (`run_experiment.py:212-215`), sin registrar la fila. No se repitió ni se imputó. Causa no determinada; la repetición queda excluida del análisis. Existe además un CSV con sólo la cabecera, `results/archive_invalid/monolith_r80_20260912_225255.csv`, de una invocación del harness a las 22:52:55, un minuto antes del inicio de esta celda: causa no determinada; excluido del análisis, como los otros tres archivos de `results/archive_invalid/` (2026-08-27).
- **Monolito 80:20, reps 26 y 27**: energía ~45% por encima del resto de la celda (0.000665 kWh vs. mediana 0.000460 kWh), valores casi idénticos entre sí. Causa no identificada — el host no estaba en uso en ese horario (madrugada). Se decidió conservar ambos valores en el dataset en vez de descartarlos, apoyándose en que los tests estadísticos empleados (Mann-Whitney U, Cliff's δ) son no paramétricos y robustos a outliers puntuales (ver "Justificación de no-parametricidad" más arriba).
