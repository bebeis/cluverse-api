#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import statistics
from collections import defaultdict
from pathlib import Path
import matplotlib.pyplot as plt


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", type=Path, default=Path(__file__).parent / "results/metrics.csv")
    parser.add_argument("--output", type=Path, default=Path(__file__).parent / "results/view-count-comparison.png")
    parser.add_argument("--workload", choices=("hot", "distributed"), default="hot")
    args = parser.parse_args()
    grouped: dict[str, dict[str, list[float]]] = defaultdict(lambda: defaultdict(list))
    with args.input.open(encoding="utf-8") as stream:
        for row in csv.DictReader(stream):
            if row["version"] not in {"v1", "v2", "v3", "v4"}:
                continue
            if row.get("workload", "hot") != args.workload:
                continue
            grouped[row["version"]]["achieved_rps"].append(float(row["achieved_rps"]))
            grouped[row["version"]]["p99_ms"].append(float(row["p99_ms"]))
    versions = sorted(grouped)
    if not versions:
        raise SystemExit(f"no rows found for workload={args.workload}")
    achieved_rps = [statistics.median(grouped[version]["achieved_rps"]) for version in versions]
    p99_ms = [statistics.median(grouped[version]["p99_ms"]) for version in versions]
    figure, axes = plt.subplots(1, 2, figsize=(11, 4.2))
    axes[0].bar(versions, achieved_rps, color="#4C78A8")
    axes[1].bar(versions, p99_ms, color="#F58518")
    axes[0].set(title=f"Achieved request rate ({args.workload})", xlabel="API version", ylabel="requests/s")
    axes[1].set(title=f"Request latency p99 ({args.workload})", xlabel="API version", ylabel="milliseconds")
    for axis in axes:
        axis.grid(axis="y", alpha=0.25)
        axis.tick_params(axis="x", rotation=0)
    figure.tight_layout()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    figure.savefig(args.output, dpi=180)
    print(args.output)


if __name__ == "__main__": main()
