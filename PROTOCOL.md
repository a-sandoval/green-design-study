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

- Energía consumida (kWh) — medida con CodeCarbon v3 vía Windows EMI/RAPL
- Emisiones de CO₂ (kg) — calculadas por CodeCarbon usando grid del SEN argentino
- Latencia p50, p95, p99 (ms) — reportadas por k6

## Variables controladas

| Variable | Valor | Justificación |
|---|---|---|
| Hardware | AMD Ryzen 7 7735HS, 16GB RAM | Mismo host para ambos brazos |
| JDK | Eclipse Temurin 21 | Misma JVM |
| Framework | Spring Boot 3.3.5 | Mismo framework y versión |
| Base de datos | PostgreSQL 16 | Misma versión y config |
| Lógica de negocio | Módulo `domain` compartido | Elimina variación en validación |
| Límites de recursos Docker | Monolito: 3 CPU + 768M; Micros: 1 CPU + 256M × 3 | Presupuesto total normalizado: 5 CPU, 1.25 GB |
| Pool de conexiones DB | Monolito: 20; Micros: 7 × 3 = 21 | Normalizado |
| Datos seed | 20 clientes + 30 productos | Idénticos en ambos brazos |
| PostgreSQL config | autovacuum=off, logging=off durante medición | Reduce ruido |
| Semilla RNG del workload | `42 + rep_number` | Reproducibilidad |

## Protocolo de ejecución

### Pre-medición

1. Cerrar todas las aplicaciones no esenciales del host
2. Desactivar actualizaciones de Windows, antivirus real-time si es posible
3. Conectar laptop a corriente (no batería)
4. Esperar 5 min de reposo antes de comenzar
5. Verificar que DVFS/turbo boost esté en estado consistente (perfil "Equilibrado")

### Por cada repetición

1. `docker compose up -d --build` — levantar el brazo
2. Health check — esperar que todos los servicios respondan 200
3. Warm-up: 120s de carga con k6 (5 VUs) — estabilizar JIT y pools
4. Iniciar CodeCarbon EmissionsTracker
5. Ejecutar k6: 60s, 10 VUs, ratio configurado, semilla fija
6. Detener CodeCarbon — registrar energía y emisiones
7. `docker compose down -v` — bajar completamente
8. Cooldown: 60s de reposo — disipar calor, normalizar CPU

### Orden de ejecución

Ejecución secuencial para evitar thermal order effects:
1. Monolito 80:20 × 30 reps
2. Microservicios 80:20 × 30 reps
3. Monolito 50:50 × 30 reps
4. Microservicios 50:50 × 30 reps
5. Monolito 20:80 × 30 reps
6. Microservicios 20:80 × 30 reps

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
- M: 0 (embodied carbon no aplica en esta comparación por ser el mismo hardware)
- R: número de requests completados exitosamente

## Amenazas a la validez

| Amenaza | Mitigación |
|---|---|
| Thermal order effects | Cooldown 60s entre reps, intercalar brazos |
| DVFS / turbo boost | Perfil "Equilibrado", reportar frecuencia media |
| GC asymmetry | JDK 21 G1GC en ambos, warm-up de 120s |
| RAPL mide package, no proceso | Cerrar apps, reportar energía idle |
| Ruido del host | 30 repeticiones, tests no paramétricos |
| CodeCarbon EMI granularidad | Muestreo cada 1s, reportar intervalo |
| Sin medidor de pared | Amenaza declarada: RAPL subestima energía total |

## Métricas SCI de referencia

- Grid intensity Argentina (SEN 2024): ~350 g CO₂/kWh
- Embodied emissions (M): 0 para esta comparación (mismo hardware)
