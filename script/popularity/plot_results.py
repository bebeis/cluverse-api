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
    parser.add_argument("--output", type=Path, default=Path(__file__).parent / "results/popularity-comparison.png")
    args = parser.parse_args()
    grouped: dict[str, dict[str, list[float]]] = defaultdict(lambda: defaultdict(list))
    with args.input.open(encoding="utf-8") as stream:
        for row in csv.DictReader(stream):
            if row["version"] not in {"v1", "v2"}:
                continue
            grouped[row["version"]]["p99_ms"].append(float(row["p99_ms"]))
            grouped[row["version"]]["examined"].append(float(row["examined_posts_avg"]))
    versions = sorted(grouped)
    if not versions:
        raise SystemExit("no v1/v2 rows found")
    p99_ms = [statistics.median(grouped[version]["p99_ms"]) for version in versions]
    examined = [statistics.median(grouped[version]["examined"]) for version in versions]
    figure, axes = plt.subplots(1, 2, figsize=(12, 4.2), constrained_layout=True)
    axes[0].bar(versions, p99_ms, color="#F58518")
    axes[1].bar(versions, examined, color="#54A24B")
    axes[1].set_yscale("log")
    axes[0].set(title="Promotion evaluation latency p99", xlabel="API version", ylabel="milliseconds")
    axes[1].set(title="Posts examined per evaluation", xlabel="API version", ylabel="posts (log scale)")
    for axis in axes:
        axis.grid(axis="y", alpha=0.25)
        axis.tick_params(axis="x", rotation=0)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    figure.savefig(args.output, dpi=180)
    print(args.output)


if __name__ == "__main__": main()
