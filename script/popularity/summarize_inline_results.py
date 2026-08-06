#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import statistics
from collections import defaultdict
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--input",
        type=Path,
        default=Path(__file__).parent / "results/inline-metrics.csv",
    )
    args = parser.parse_args()

    grouped: dict[tuple[str, str], dict[str, list[float]]] = defaultdict(
        lambda: defaultdict(list)
    )
    with args.input.open(encoding="utf-8") as stream:
        for row in csv.DictReader(stream):
            key = (row["kind"], row["condition"])
            grouped[key]["p95_ms"].append(float(row["p95_ms"]))
            grouped[key]["p99_ms"].append(float(row["p99_ms"]))
            grouped[key]["failure_rate"].append(float(row["failure_rate"]))
            grouped[key]["dropped_iterations"].append(float(row["dropped_iterations"]))

    print("| API | inline off p95/p99 | inline on p95/p99 | p95/p99 delta | failure / dropped |")
    print("|---|---:|---:|---:|---:|")
    for kind in ("like", "comment"):
        disabled = grouped.get((kind, "disabled"))
        enabled = grouped.get((kind, "enabled"))
        if not disabled or not enabled:
            raise SystemExit(f"{kind}의 disabled/enabled 결과가 모두 필요합니다.")
        disabled_p95 = statistics.median(disabled["p95_ms"])
        disabled_p99 = statistics.median(disabled["p99_ms"])
        enabled_p95 = statistics.median(enabled["p95_ms"])
        enabled_p99 = statistics.median(enabled["p99_ms"])
        p95_delta = enabled_p95 - disabled_p95
        p99_delta = enabled_p99 - disabled_p99
        failure = max(disabled["failure_rate"] + enabled["failure_rate"])
        dropped = max(disabled["dropped_iterations"] + enabled["dropped_iterations"])
        print(
            f"| {kind} | {disabled_p95:.2f}/{disabled_p99:.2f} ms "
            f"| {enabled_p95:.2f}/{enabled_p99:.2f} ms "
            f"| {p95_delta:+.2f}/{p99_delta:+.2f} ms | {failure:.4f} / {dropped:.0f} |"
        )


if __name__ == "__main__":
    main()
