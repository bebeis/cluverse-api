#!/usr/bin/env python3
from __future__ import annotations

import csv
import json
import re
import sys
from pathlib import Path


def metric_value(metrics: dict, name: str, key: str, default: float = 0.0) -> float:
    metric = metrics.get(name, {})
    values = metric.get("values", metric)
    if key in values:
        return float(values[key])
    if key == "rate" and "value" in values:
        return float(values["value"])
    return float(default)


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("usage: collect_inline_results.py <k6-summary.json>")
    path = Path(sys.argv[1])
    match = re.search(r"-inline-(disabled|enabled)-(like|comment)-r(\d+)-summary$", path.stem)
    if not match:
        raise SystemExit(f"unexpected summary filename: {path.name}")

    data = json.loads(path.read_text(encoding="utf-8"))
    metrics = data.get("metrics", {})
    row = {
        "run": path.stem,
        "condition": match.group(1),
        "kind": match.group(2),
        "repeat": int(match.group(3)),
        "avg_ms": metric_value(metrics, "popularity_inline_api_duration", "avg"),
        "p95_ms": metric_value(metrics, "popularity_inline_api_duration", "p(95)"),
        "p99_ms": metric_value(metrics, "popularity_inline_api_duration", "p(99)"),
        "max_ms": metric_value(metrics, "popularity_inline_api_duration", "max"),
        "failure_rate": metric_value(metrics, "popularity_inline_failures", "rate"),
        "dropped_iterations": metric_value(metrics, "dropped_iterations", "count"),
        "completed": metric_value(metrics, "popularity_inline_completed", "count"),
    }
    output = path.parents[1] / "inline-metrics.csv"
    with output.open("a", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(row))
        if output.stat().st_size == 0:
            writer.writeheader()
        writer.writerow(row)
    print(json.dumps(row, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
