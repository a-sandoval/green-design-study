"""
Ejecuta el experimento completo: 2 brazos × 3 ratios × N repeticiones.
Uso: python harness/run_all.py --reps 30
"""

import subprocess
import sys
from pathlib import Path

HARNESS = Path(__file__).resolve().parent / "run_experiment.py"

ARMS = ["monolith", "microservices"]
READ_RATIOS = [80, 50, 20]


def main():
    reps = 30
    if len(sys.argv) > 2 and sys.argv[1] == "--reps":
        reps = int(sys.argv[2])

    for arm in ARMS:
        for ratio in READ_RATIOS:
            print(f"\n{'='*60}")
            print(f"  EJECUTANDO: {arm} | ratio L:E = {ratio}:{100-ratio}")
            print(f"{'='*60}\n")
            subprocess.run([
                sys.executable, str(HARNESS),
                "--arm", arm,
                "--reps", str(reps),
                "--read-ratio", str(ratio),
            ], check=True)

    print("\n=== EXPERIMENTO COMPLETO ===")


if __name__ == "__main__":
    main()
