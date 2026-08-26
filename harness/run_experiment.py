"""
Harness de medición para el experimento Green Design.

Orquesta:
1. Levanta el brazo (monolito o microservicios) via docker-compose
2. Espera warm-up (JVM JIT + conexiones)
3. Inicia medición con CodeCarbon (Windows EMI/RAPL)
4. Ejecuta carga de trabajo con k6
5. Detiene medición
6. Registra métricas (energía, latencia)
7. Baja el brazo
8. Cooldown antes de la siguiente repetición

Uso:
    python harness/run_experiment.py --arm monolith --reps 30 --read-ratio 80
    python harness/run_experiment.py --arm microservices --reps 30 --read-ratio 80
"""

import argparse
import csv
import json
import os
import subprocess
import sys
import time
from datetime import datetime
from pathlib import Path

try:
    from codecarbon import EmissionsTracker
except ImportError:
    print("ERROR: codecarbon no instalado. Ejecutar: pip install codecarbon")
    sys.exit(1)


PROJECT_ROOT = Path(__file__).resolve().parent.parent
RESULTS_DIR = PROJECT_ROOT / "results"
K6_SCENARIO = PROJECT_ROOT / "workload" / "k6" / "scenario.js"

ARM_CONFIG = {
    "monolith": {
        "compose_dir": PROJECT_ROOT / "monolith",
        "base_url": "http://localhost:8080",
        "clientes_url": "http://localhost:8080",
        "productos_url": "http://localhost:8080",
        "health_url": "http://localhost:8080/api/clientes",
    },
    "microservices": {
        "compose_dir": PROJECT_ROOT / "microservices",
        "base_url": "http://localhost:8083",
        "clientes_url": "http://localhost:8081",
        "productos_url": "http://localhost:8082",
        "health_url": "http://localhost:8081/api/clientes",
    },
}


def run_cmd(cmd, cwd=None, check=True, capture=False):
    result = subprocess.run(
        cmd, cwd=cwd, check=check, shell=True,
        capture_output=capture, text=True if capture else None,
    )
    return result


def docker_compose_up(compose_dir):
    print(f"  Levantando containers en {compose_dir}...")
    run_cmd("docker compose up -d --build", cwd=compose_dir)


def docker_compose_down(compose_dir):
    print(f"  Bajando containers en {compose_dir}...")
    run_cmd("docker compose down -v", cwd=compose_dir)


def wait_for_health(url, timeout=120):
    import urllib.request
    import urllib.error
    print(f"  Esperando health check en {url}...")
    start = time.time()
    while time.time() - start < timeout:
        try:
            req = urllib.request.Request(url)
            with urllib.request.urlopen(req, timeout=5) as resp:
                if resp.status == 200:
                    print(f"  Servicio listo ({time.time() - start:.1f}s)")
                    return True
        except (urllib.error.URLError, ConnectionError, OSError):
            pass
        time.sleep(2)
    raise TimeoutError(f"Servicio no respondió en {timeout}s: {url}")


def k6_env_args(config):
    return (
        f'--env BASE_URL={config["base_url"]} '
        f'--env CLIENTES_URL={config["clientes_url"]} '
        f'--env PRODUCTOS_URL={config["productos_url"]}'
    )


def run_warmup(config, seconds):
    print(f"  Warm-up: {seconds}s de carga ligera...")
    run_cmd(
        f'k6 run {k6_env_args(config)} --env READ_RATIO=80 '
        f'--env DURATION={seconds}s --env VUS=5 --env SEED=1 '
        f'"{K6_SCENARIO}" --quiet',
    )


def run_workload(config, read_ratio, duration, vus, seed):
    print(f"  Ejecutando carga: {duration}s, {vus} VUs, L:E={read_ratio}:{100 - read_ratio}, seed={seed}")
    result = run_cmd(
        f'k6 run {k6_env_args(config)} --env READ_RATIO={read_ratio} '
        f'--env DURATION={duration}s --env VUS={vus} --env SEED={seed} '
        f'"{K6_SCENARIO}" --quiet',
        capture=True,
        check=False,
    )
    return result.stdout


def parse_args():
    p = argparse.ArgumentParser(description="Green Design Experiment Harness")
    p.add_argument("--arm", required=True, choices=["monolith", "microservices"])
    p.add_argument("--reps", type=int, default=30)
    p.add_argument("--read-ratio", type=int, default=80)
    p.add_argument("--warmup-seconds", type=int, default=120)
    p.add_argument("--duration", type=int, default=60)
    p.add_argument("--vus", type=int, default=10)
    p.add_argument("--cooldown-seconds", type=int, default=60)
    p.add_argument("--seed-base", type=int, default=42)
    return p.parse_args()


def main():
    args = parse_args()
    config = ARM_CONFIG[args.arm]

    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    run_id = f"{args.arm}_r{args.read_ratio}_{timestamp}"
    results_path = RESULTS_DIR / f"{run_id}.csv"
    RESULTS_DIR.mkdir(parents=True, exist_ok=True)

    print(f"=== Experimento: {run_id} ===")
    print(f"  Brazo: {args.arm}")
    print(f"  Repeticiones: {args.reps}")
    print(f"  Ratio L:E: {args.read_ratio}:{100 - args.read_ratio}")
    print(f"  Duración por rep: {args.duration}s")
    print(f"  VUs: {args.vus}")
    print(f"  Warm-up: {args.warmup_seconds}s")
    print(f"  Cooldown: {args.cooldown_seconds}s")
    print()

    fieldnames = [
        "run_id", "arm", "rep", "read_ratio", "seed",
        "energy_kwh", "emissions_kg_co2", "duration_s",
        "k6_stdout",
    ]

    with open(results_path, "w", newline="", encoding="utf-8") as csvfile:
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()

        for rep in range(1, args.reps + 1):
            seed = args.seed_base + rep
            print(f"\n--- Repetición {rep}/{args.reps} (seed={seed}) ---")

            docker_compose_up(config["compose_dir"])

            try:
                wait_for_health(config["health_url"])

                if args.arm == "microservices":
                    wait_for_health("http://localhost:8082/api/productos")
                    wait_for_health("http://localhost:8083/api/pedidos")

                run_warmup(config, args.warmup_seconds)

                tracker = EmissionsTracker(
                    project_name=run_id,
                    output_dir=str(RESULTS_DIR),
                    log_level="warning",
                    save_to_file=False,
                )
                tracker.start()

                k6_output = run_workload(
                    config, args.read_ratio,
                    args.duration, args.vus, seed,
                )

                emissions = tracker.stop()

                row = {
                    "run_id": run_id,
                    "arm": args.arm,
                    "rep": rep,
                    "read_ratio": args.read_ratio,
                    "seed": seed,
                    "energy_kwh": tracker._total_energy.kWh if hasattr(tracker, '_total_energy') else "",
                    "emissions_kg_co2": emissions if emissions is not None else "",
                    "duration_s": args.duration,
                    "k6_stdout": k6_output.strip() if k6_output else "",
                }
                writer.writerow(row)
                csvfile.flush()

                print(f"  Energía: {row['energy_kwh']} kWh")
                print(f"  Emisiones: {row['emissions_kg_co2']} kg CO2")

            except Exception as e:
                print(f"  ERROR en rep {rep}: {e}")
            finally:
                docker_compose_down(config["compose_dir"])

            if rep < args.reps:
                print(f"  Cooldown: {args.cooldown_seconds}s...")
                time.sleep(args.cooldown_seconds)

    print(f"\n=== Resultados guardados en {results_path} ===")


if __name__ == "__main__":
    main()
