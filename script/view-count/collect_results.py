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
    match = re.search(r"-(bench|correctness)-(v[1-4])(?:-(hot|distributed))?", path.stem)
    workload = "unknown"
    if match:
        workload = (match.group(3) or "hot") if match.group(1) == "bench" else "correctness"
    requests = value(metrics, "http_reqs", "count")
    row = {
        "run": path.stem,
        "version": match.group(2) if match else "unknown",
        "workload": workload,
        "requests": int(requests),
        "achieved_rps": value(metrics, "http_reqs", "rate"),
        "p95_ms": value(metrics, "view_count_duration", "p(95)"),
        "p99_ms": value(metrics, "view_count_duration", "p(99)"),
        "failure_rate": value(metrics, "view_count_failures", "rate", "value"),
    }
    output = path.parents[1] / "metrics.csv"
    with output.open("a", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(row))
        if output.stat().st_size == 0:
            writer.writeheader()
        writer.writerow(row)
    print(json.dumps(row, ensure_ascii=False, indent=2))


if __name__ == "__main__": main()
