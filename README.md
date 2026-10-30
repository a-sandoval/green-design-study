# Green Design Study

Estudio empírico del consumo energético de un sistema CRUD de e-commerce implementado como monolito modular y como microservicios (base de datos por servicio), con Java 21, Spring Boot y PostgreSQL.

## Relación con el artículo

Este repositorio respalda el artículo «Green Design» (CoNaIISI 2026). El artículo cita una revisión fija del repositorio.

| Contenido | Ubicación |
|---|---|
| Protocolo experimental: diseño, ejecución tal como se corrió, amenazas a la validez y desviaciones | [`PROTOCOL.md`](PROTOCOL.md) |
| Validación funcional previa a la campaña (fallo detectado, diagnóstico, corrección y nueva prueba) | [`docs/validacion-funcional.md`](docs/validacion-funcional.md) |
| Harness de medición | [`harness/run_experiment.py`](harness/run_experiment.py), [`harness/run_all.py`](harness/run_all.py) |
| Carga de trabajo (k6) | [`workload/k6/scenario.js`](workload/k6/scenario.js) |
| Análisis estadístico | [`analysis/analyze.py`](analysis/analyze.py) |
| Datos de la campaña: seis CSV (179 repeticiones registradas de 180) | [Carpeta en Drive](https://drive.google.com/drive/folders/1OkSWptK3YBtjzAoj6QezCfmwL80tFAM1) |

El directorio `results/` de una ejecución local no está versionado: los CSV de la campaña se distribuyen únicamente por el enlace de Drive.
