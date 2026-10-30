"""
Análisis estadístico para Green Design Study.

Produce:
- Tabla descriptiva (mediana, IQR, min, max) por brazo × ratio
- Mann-Whitney U test entre brazos para cada ratio
- Cliff's delta (tamaño de efecto ordinal)
- Vargha-Delaney Â₁₂ (probabilidad de superioridad)
- Figuras: boxplots energía por brazo×ratio, violín, barras SCI

Uso:
    python analysis/analyze.py --results-dir results/
"""

import argparse
import csv
import sys
from collections import defaultdict
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

import numpy as np

try:
    from scipy import stats
except ImportError:
    print("ERROR: scipy no instalado. Ejecutar: pip install scipy")
    sys.exit(1)

try:
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
except ImportError:
    print("ERROR: matplotlib no instalado. Ejecutar: pip install matplotlib")
    sys.exit(1)


def load_results(results_dir):
    data = defaultdict(list)
    results_path = Path(results_dir)
    for csv_file in results_path.glob("*.csv"):
        with open(csv_file, newline="", encoding="utf-8") as f:
            reader = csv.DictReader(f)
            for row in reader:
                if not row.get("energy_kwh"):
                    continue
                arm = row["arm"]
                ratio = int(row["read_ratio"])
                energy = float(row["energy_kwh"])
                data[(arm, ratio)].append(energy)
    return data


def cliffs_delta(x, y):
    n_x, n_y = len(x), len(y)
    more = sum(1 for xi in x for yj in y if xi > yj)
    less = sum(1 for xi in x for yj in y if xi < yj)
    d = (more - less) / (n_x * n_y)
    return d


def cliffs_delta_magnitude(d):
    d_abs = abs(d)
    if d_abs < 0.147:
        return "negligible"
    elif d_abs < 0.33:
        return "small"
    elif d_abs < 0.474:
        return "medium"
    else:
        return "large"


def vargha_delaney(x, y):
    n_x, n_y = len(x), len(y)
    more = sum(1 for xi in x for yj in y if xi > yj)
    equal = sum(1 for xi in x for yj in y if xi == yj)
    a12 = (more + 0.5 * equal) / (n_x * n_y)
    return a12


def descriptive_table(data):
    print("\n" + "=" * 80)
    print("TABLA DESCRIPTIVA — Energía (kWh)")
    print("=" * 80)
    print(f"{'Brazo':<15} {'Ratio L:E':<12} {'N':>4} {'Mediana':>12} {'IQR':>12} {'Min':>12} {'Max':>12}")
    print("-" * 80)
    for (arm, ratio) in sorted(data.keys()):
        values = np.array(data[(arm, ratio)])
        n = len(values)
        med = np.median(values)
        q1 = np.percentile(values, 25)
        q3 = np.percentile(values, 75)
        iqr = q3 - q1
        print(f"{arm:<15} {ratio}:{100-ratio:<10} {n:>4} {med:>12.6f} {iqr:>12.6f} {np.min(values):>12.6f} {np.max(values):>12.6f}")


def statistical_tests(data):
    print("\n" + "=" * 80)
    print("TESTS ESTADÍSTICOS — Monolito vs Microservicios")
    print("=" * 80)
    print(f"{'Ratio L:E':<12} {'U':>10} {'p-value':>12} {'Cliff δ':>10} {'Magnitud':<12} {'Â₁₂':>8} {'Interpretación'}")
    print("-" * 100)

    ratios = sorted(set(r for (_, r) in data.keys()))
    for ratio in ratios:
        mono = data.get(("monolith", ratio), [])
        micro = data.get(("microservices", ratio), [])
        if not mono or not micro:
            print(f"{ratio}:{100-ratio:<10}  — datos insuficientes —")
            continue

        mono_arr = np.array(mono)
        micro_arr = np.array(micro)

        u_stat, p_value = stats.mannwhitneyu(mono_arr, micro_arr, alternative="two-sided")
        delta = cliffs_delta(micro_arr, mono_arr)
        mag = cliffs_delta_magnitude(delta)
        a12 = vargha_delaney(micro_arr, mono_arr)

        if a12 > 0.5:
            interp = "micro > mono"
        elif a12 < 0.5:
            interp = "mono > micro"
        else:
            interp = "equal"

        sig = "***" if p_value < 0.001 else "**" if p_value < 0.01 else "*" if p_value < 0.05 else "ns"

        print(f"{ratio}:{100-ratio:<10} {u_stat:>10.1f} {p_value:>11.6f}{sig:>2} {delta:>10.4f} {mag:<12} {a12:>8.4f} {interp}")


def plot_boxplots(data, output_dir):
    ratios = sorted(set(r for (_, r) in data.keys()))
    fig, axes = plt.subplots(1, len(ratios), figsize=(5 * len(ratios), 6), sharey=True)
    if len(ratios) == 1:
        axes = [axes]

    for ax, ratio in zip(axes, ratios):
        mono = data.get(("monolith", ratio), [])
        micro = data.get(("microservices", ratio), [])

        bp = ax.boxplot(
            [mono, micro],
            tick_labels=["Monolito", "Microservicios"],
            patch_artist=True,
            widths=0.5,
        )
        bp["boxes"][0].set_facecolor("#2196F3")
        bp["boxes"][1].set_facecolor("#FF9800")
        ax.set_title(f"Ratio L:E = {ratio}:{100-ratio}")
        ax.set_ylabel("Energía (kWh)" if ax == axes[0] else "")
        ax.grid(axis="y", alpha=0.3)

    fig.suptitle("Consumo Energético: Monolito vs Microservicios", fontsize=14, fontweight="bold")
    plt.tight_layout()
    out = Path(output_dir) / "energy_boxplots.png"
    fig.savefig(out, dpi=150, bbox_inches="tight")
    plt.close()
    print(f"\nBoxplot guardado en {out}")


def plot_violin(data, output_dir):
    ratios = sorted(set(r for (_, r) in data.keys()))
    fig, axes = plt.subplots(1, len(ratios), figsize=(5 * len(ratios), 6), sharey=True)
    if len(ratios) == 1:
        axes = [axes]

    for ax, ratio in zip(axes, ratios):
        mono = data.get(("monolith", ratio), [])
        micro = data.get(("microservices", ratio), [])
        if not mono or not micro:
            continue

        parts = ax.violinplot([mono, micro], positions=[1, 2], showmedians=True, showextrema=True)
        colors = ["#2196F3", "#FF9800"]
        for pc, color in zip(parts["bodies"], colors):
            pc.set_facecolor(color)
            pc.set_alpha(0.7)

        ax.set_xticks([1, 2])
        ax.set_xticklabels(["Monolito", "Microservicios"])
        ax.set_title(f"Ratio L:E = {ratio}:{100-ratio}")
        ax.set_ylabel("Energía (kWh)" if ax == axes[0] else "")
        ax.grid(axis="y", alpha=0.3)

    fig.suptitle("Distribución de Consumo Energético", fontsize=14, fontweight="bold")
    plt.tight_layout()
    out = Path(output_dir) / "energy_violin.png"
    fig.savefig(out, dpi=150, bbox_inches="tight")
    plt.close()
    print(f"Violín guardado en {out}")


def plot_sci_bars(data, output_dir, grid_intensity=350):
    ratios = sorted(set(r for (_, r) in data.keys()))
    mono_sci = []
    micro_sci = []
    labels = []

    for ratio in ratios:
        mono = data.get(("monolith", ratio), [])
        micro = data.get(("microservices", ratio), [])
        if not mono or not micro:
            continue

        mono_med = np.median(mono) * 1000 * grid_intensity
        micro_med = np.median(micro) * 1000 * grid_intensity
        mono_sci.append(mono_med)
        micro_sci.append(micro_med)
        labels.append(f"{ratio}:{100-ratio}")

    x = np.arange(len(labels))
    width = 0.35

    fig, ax = plt.subplots(figsize=(8, 6))
    bars1 = ax.bar(x - width / 2, mono_sci, width, label="Monolito", color="#2196F3")
    bars2 = ax.bar(x + width / 2, micro_sci, width, label="Microservicios", color="#FF9800")

    ax.set_xlabel("Ratio Lectura:Escritura")
    ax.set_ylabel("SCI (g CO₂ por ejecución)")
    ax.set_title("Software Carbon Intensity por Brazo y Ratio")
    ax.set_xticks(x)
    ax.set_xticklabels(labels)
    ax.legend()
    ax.grid(axis="y", alpha=0.3)

    plt.tight_layout()
    out = Path(output_dir) / "sci_bars.png"
    fig.savefig(out, dpi=150, bbox_inches="tight")
    plt.close()
    print(f"Barras SCI guardadas en {out}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--results-dir", default="results")
    parser.add_argument("--output-dir", default="analysis/figures")
    parser.add_argument("--grid-intensity", type=float, default=350,
                        help="Grid carbon intensity (g CO2/kWh)")
    args = parser.parse_args()

    Path(args.output_dir).mkdir(parents=True, exist_ok=True)

    data = load_results(args.results_dir)
    if not data:
        print("No se encontraron resultados en", args.results_dir)
        sys.exit(1)

    descriptive_table(data)
    statistical_tests(data)
    plot_boxplots(data, args.output_dir)
    plot_violin(data, args.output_dir)
    plot_sci_bars(data, args.output_dir, args.grid_intensity)

    print("\nAnálisis completo.")


if __name__ == "__main__":
    main()
