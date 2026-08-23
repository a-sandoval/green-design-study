"""
Harness de medición para el experimento Green Design.

Orquesta:
1. Levanta el brazo (monolito o microservicios) via docker-compose
2. Espera warm-up (JVM JIT + conexiones)
3. Inicia medición con CodeCarbon (Windows EMI/RAPL)
4. Ejecuta carga de trabajo con k6/generador
5. Detiene medición
6. Registra métricas (energía, CPU, memoria, latencia, queries)
7. Baja el brazo
8. Cooldown antes de la siguiente repetición

Uso:
    python run_experiment.py --arm monolith --repetitions 30 --warmup-seconds 120
    python run_experiment.py --arm microservices --repetitions 30 --warmup-seconds 120
"""
# TODO: implementar en Fase 4
