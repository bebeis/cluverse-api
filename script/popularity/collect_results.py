#!/usr/bin/env python3
from __future__ import annotations

import csv
import json
import re
import sys
from pathlib import Path


def value(metrics: dict, name: str, *keys: str, default: float = 0.0) -> float:
    metric = metrics.get(name, {})
    values = metric.get("values", metric)
    for key in keys:
        if key in values:
            return float(values[key])
    return default


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("usage: collect_results.py <k6-summary.json>")
    path = Path(sys.argv[1])
    data = json.loads(path.read_text(encoding="utf-8"))
    metrics = data.get("metrics", {})
    match = re.search(r"-bench-(v[12])", path.stem)
    row = {
        "run": path.stem,
        "version": match.group(1) if match else "unknown",
        "p95_ms": value(metrics, "popularity_evaluation_duration", "p(95)"),
        "p99_ms": value(metrics, "popularity_evaluation_duration", "p(99)"),
        "examined_posts_avg": value(metrics, "popularity_examined_posts", "avg"),
        "failure_rate": value(metrics, "popularity_failures", "rate", "value"),
    }
    output = path.parents[1] / "metrics.csv"
    with output.open("a", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(row))
        if output.stat().st_size == 0:
            writer.writeheader()
        writer.writerow(row)
    print(json.dumps(row, ensure_ascii=False, indent=2))


if __name__ == "__main__": main()
