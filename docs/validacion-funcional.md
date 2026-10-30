# Validación funcional del harness antes de la campaña

Registro de la prueba previa que detectó un fallo de generación de carga, su diagnóstico, su corrección y su revalidación, ocurridos el 2026-09-12 antes del lanzamiento de la campaña de 180 repeticiones (primera invocación del harness a las 22:52:55; primera celda a las 22:53:48).

La corrección descrita se aplicó localmente el 2026-09-12, antes de lanzar la campaña, y se commiteó el 2026-10-30 (commit `6730637`, sección 6): la campaña se ejecutó desde un árbol de trabajo con ese cambio sin commitear.

## 1. Resumen

| Etapa | Resultado |
|---|---|
| Prueba previa (monolito, 80:20, repetición 1, semilla 43) | 163 de 5.697 solicitudes fallidas (2,86 %), todas escrituras |
| Diagnóstico | Altas de cliente con email repetido respecto del warm-up; violación de `UNIQUE(email)` devuelta como HTTP 500 |
| Reproducción controlada | Segundo alta con el mismo email: HTTP 500. Reconstrucción de la generación de emails: 163 colisiones, igual que los fallos observados |
| Corrección | El email se construye con la semilla, el VU y la iteración, sin pasar por el generador pseudoaleatorio |
| Nueva prueba | 0 de 5.670 solicitudes fallidas (0,00 %) |
| Campaña (179 filas registradas) | 0 filas con errores de solicitud |

## 2. Fuentes de evidencia

| Dato | Fuente | ¿Verificable desde el repositorio? |
|---|---|---|
| Código que originó el fallo | Commit `560ee3a` (2026-08-23) y `d2d8c4e` (último commit previo a la corrección) | Sí |
| Corrección de `scenario.js` | Cambio de una línea aplicado localmente el 2026-09-12, antes de la campaña; commiteado el 2026-10-30 como `673063749d35938bf7914e2106b5c079dca531ba` | Sí, desde ese commit |
| Cifras del piloto (sección 3) | Salida de consola y CSV del piloto (`run_id` `monolith_r80_20260912_222245`) observados el 2026-09-12 | No: el CSV fue eliminado el 2026-09-12; las cifras provienen del registro de consola de esa sesión |
| Reproducción controlada y revalidación (secciones 4 y 6) | Salida de consola del 2026-09-12 | No |
| Reconstrucción de la generación de emails | Fragmento de la sección 5, ejecutable | Sí |
| Ausencia de errores en la campaña | `k6_stdout` de los 179 registros de `results/` (CSV en Drive) | Sí, sobre los CSV |

## 3. Antecedentes y prueba previa

Correcciones funcionales anteriores presentes en el historial de git:

- `7720f6e` (2026-08-24): stock inicial elevado a 999999 «para evitar el agotamiento de stock durante la medición».
- `f2d2c7f` (2026-08-25): enrutamiento de k6 por servicio en el brazo de microservicios.

Ejecuciones de prueba descartadas, conservadas en `results/archive_invalid/` (no versionado): tres del 2026-08-27 (00:00:35 y 00:08:19, sólo cabecera; 00:01:10, una fila con las 5.930 solicitudes fallidas y 0 B recibidos) y una del 2026-09-12 a las 22:52:55 (sólo cabecera). Causa no determinada; excluidas del análisis.

**Prueba previa del 2026-09-12** (`harness/run_experiment.py --arm monolith --reps 1 --read-ratio 80`; `run_id` `monolith_r80_20260912_222245`, iniciada a las 22:22:45):

| Fase | Configuración | Resultado |
|---|---|---|
| Warm-up | 120 s, 5 VUs, semilla 1 | 5.651 solicitudes, 0,00 % de errores |
| Medición | 60 s, 10 VUs, semilla 43, ratio 80:20 | 5.697 solicitudes; 163 fallidas (2,86 %) |

En la medición: 1.143 escrituras (980 correctas, 163 fallidas) y 4.554 lecturas sin fallos (derivado de 5.697 − 1.143). El umbral de k6 (`errors: rate<0.05`, `workload/k6/scenario.js`) no se activó, porque 2,86 % < 5 %, y el harness no evalúa el resultado de k6: ejecuta `k6 run` con `check=False` y sólo guarda el texto de salida (`run_experiment.py:111-120`). El fallo sólo era visible al leer ese resumen.

## 4. Diagnóstico

La cadena causal, con el código del commit `d2d8c4e` (previo a la corrección; los números de línea de `scenario.js` son los de esa revisión):

1. **Esquema**: `clientes.email` es `UNIQUE` en ambos esquemas (`infra/postgres/init-monolith.sql:7`, `init-microservices.sql:8`).
2. **Sin manejo de la violación**: ningún controlador ni `@ControllerAdvice` captura `DataIntegrityViolationException` (`git grep` sobre `*.java` no encuentra manejadores). Un alta con email repetido responde HTTP 500 en lugar de un código de conflicto.
3. **Generación de emails en k6**: `email: k6_${randomInt(1, 99999)}@test.com` (`workload/k6/scenario.js:85`). `randomInt` usa un congruencial lineal propio (`scenario.js:38-42`, multiplicador 1103515245) que se **reinicializa en cada iteración** con `SEED + __VU * 10000 + __ITER` (`scenario.js:109`), de modo que cada iteración consume sólo unos pocos pasos del generador.
4. **Warm-up con otra semilla pero el mismo espacio de emails**: el warm-up corre con `SEED=1` y 5 VUs (`run_experiment.py:105-106`); la medición, con `SEED=42+rep` (`run_experiment.py:115,166`). Ambas fases insertan en la misma base. Los emails del warm-up ya existen cuando comienza la medición, y una parte de los emails generados en la medición coincide con ellos.

Las líneas citadas existen sin cambios desde el commit `560ee3a` (2026-08-23); `scenario.js` sólo cambió después, en `f2d2c7f`, en el enrutamiento de URLs.

## 5. Reproducción controlada

**Alta duplicada.** Sobre el monolito levantado con Docker, dos `POST /api/clientes` con el mismo email: el primero respondió `201` (cliente `id` 21); el segundo, `500` (`{"status":500,"error":"Internal Server Error","path":"/api/clientes"}`).

**Reconstrucción de la generación de emails.** Se reprodujo la lógica de `scenario.js` fuera de k6. Detalle relevante: en JavaScript los números son de doble precisión y `rngState * 1103515245` supera 2^53 a partir del segundo paso, de modo que el generador real no coincide con un congruencial en aritmética entera exacta. Con enteros exactos la misma lógica da 116 coincidencias de 258 altas; emulando doble precisión da 163 de 263, igual a los 163 fallos observados.

```python
def make_rng(seed):
    state = [float(seed)]
    def next_rng():
        x = state[0] * 1103515245.0 + 12345.0          # float64, como en JavaScript
        state[0] = float(int(x) & 0x7fffffff)
        return int(state[0])
    return next_rng

def rand_int(rng, lo, hi):
    return lo + (rng() % (hi - lo + 1))

def emails_de_altas_de_cliente(seed, vus, iteraciones_por_vu, read_ratio=80):
    emails = []
    for vu in range(1, vus + 1):                        # __VU arranca en 1
        for it in range(iteraciones_por_vu):            # __ITER arranca en 0
            rng = make_rng(seed + vu * 10000 + it)      # scenario.js:109
            if rand_int(rng, 1, 100) <= read_ratio:     # lectura
                continue
            op = rand_int(rng, 1, 100)
            if 50 < op <= 75:                           # alta de cliente
                rand_int(rng, 1, 99999)                 # nombre
                emails.append(rand_int(rng, 1, 99999))  # email
    return emails

calentamiento = set(emails_de_altas_de_cliente(1, 5, 1130))   # 5.651 iteraciones / 5 VUs
medicion = emails_de_altas_de_cliente(43, 10, 570)            # 5.697 iteraciones / 10 VUs
print(len(medicion), sum(e in calentamiento for e in medicion))   # -> 263 163
```

Las iteraciones por VU se estimaron dividiendo las iteraciones totales observadas (5.651 y 5.697) por la cantidad de VUs; con 566 a 574 iteraciones por VU en la medición, las colisiones van de 163 a 165. No hubo emails repetidos dentro de la propia medición.

## 6. Corrección y nueva prueba

**Corrección** (`workload/k6/scenario.js`, una línea):

```diff
-      email: `k6_${randomInt(1, 99999)}@test.com`,
+      email: `k6_${SEED}_${__VU}_${__ITER}@test.com`,
```

El email pasa a ser una función determinista de (semilla, VU, iteración), única dentro de una ejecución de k6 y distinta entre el warm-up (`SEED=1`) y las mediciones (`SEED` ≥ 43), con independencia del generador pseudoaleatorio. No se modificó el manejo de la violación de `UNIQUE` en el backend: un email repetido seguiría devolviendo HTTP 500.

**Cronología de la corrección.** Se aplicó localmente el 2026-09-12, antes de lanzar la campaña (primera invocación del harness a las 22:52:55), y se commiteó después de ella, el 2026-10-30, en el commit `673063749d35938bf7914e2106b5c079dca531ba` (`6730637`). La campaña se ejecutó, por lo tanto, desde un árbol de trabajo con ese cambio sin commitear sobre el commit `d2d8c4e`; ninguna revisión commiteada anterior a `6730637` coincide con el código que se ejecutó.

**Nueva prueba** (2026-09-12, posterior a la prueba previa de las 22:22:45 y anterior a la primera invocación del harness de las 22:52:55). Se ejecutó `k6` directamente contra el monolito, fuera del harness, con las mismas semillas que fallaron:

| Fase | Configuración | Resultado |
|---|---|---|
| Warm-up | 30 s, 5 VUs, semilla 1 | 1.398 iteraciones |
| Medición | 60 s, 10 VUs, semilla 43, ratio 80:20 | 5.670 solicitudes; 0 fallidas (**0,00 %**) |

Un intento anterior de la misma verificación se descartó: el warm-up terminó con 0 de 1.051 solicitudes fallidas, pero la medición no generó solicitudes porque el demonio de Docker dejó de responder; se reinició Docker Desktop y la verificación se repitió completa.

## 7. Resultado en la campaña

Sobre los 179 registros de `results/` (seis CSV), ninguna fila tiene solicitudes con error en su `k6_stdout`; las solicitudes por ventana de 60 s van de 5.445 a 5.830.

## 8. Alcance y límites

- La revalidación se hizo en el brazo monolito, con ratio 80:20, con un warm-up de 30 s (el harness usa 120 s) y sin pasar por el harness. La campaña posterior, con 0 filas con errores en ambos brazos y los tres ratios, es la evidencia a escala de que la corrección se sostuvo.
- El diagnóstico se basa en el código del commit `d2d8c4e` y en la reconstrucción de la sección 5. Las cifras de la prueba previa dependen de un archivo que no se conserva (ver sección 2).
- La corrección elimina la causa en la generación de carga; el backend sigue devolviendo HTTP 500 ante un email repetido.
