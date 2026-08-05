#!/usr/bin/env python3
"""Export selected Prometheus range queries to capture-friendly CSV and raw JSON."""

from __future__ import annotations

import argparse
import csv
import json
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


CSV_FIELDS = (
    "timestamp",
    "recorded_at",
    "elapsed_seconds",
    "metric",
    "title",
    "group",
    "value",
    "unit",
    "labels",
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Prometheus 병목 지표를 query_range로 수집합니다.")
    parser.add_argument("--url", default="http://localhost:9090")
    parser.add_argument("--start", required=True, type=float, help="Unix epoch seconds")
    parser.add_argument("--end", required=True, type=float, help="Unix epoch seconds")
    parser.add_argument("--origin", type=float, help="elapsed_seconds=0으로 사용할 테스트 시작 epoch")
    parser.add_argument("--step", default=15, type=int)
    parser.add_argument(
        "--queries",
        type=Path,
        default=Path(__file__).with_name("prometheus") / "queries.json",
    )
    parser.add_argument("--output-dir", required=True, type=Path)
    parser.add_argument("--strict", action="store_true", help="하나의 query라도 실패하면 종료 코드 1")
    return parser.parse_args()


def query_range(base_url: str, query: str, start: float, end: float, step: int) -> dict[str, Any]:
    parameters = urllib.parse.urlencode({"query": query, "start": start, "end": end, "step": step})
    url = f"{base_url.rstrip('/')}/api/v1/query_range?{parameters}"
    request = urllib.request.Request(url, headers={"Accept": "application/json"})
    with urllib.request.urlopen(request, timeout=30) as response:
        payload = json.load(response)
    if payload.get("status") != "success":
        raise ValueError(payload.get("error") or "Prometheus query가 실패했습니다.")
    return payload


def flatten_result(spec: dict[str, str], payload: dict[str, Any], start: float) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    results = payload.get("data", {}).get("result", [])
    for series in results:
        labels = json.dumps(series.get("metric", {}), ensure_ascii=False, sort_keys=True)
        for timestamp, raw_value in series.get("values", []):
            value = float(raw_value)
            rows.append({
                "timestamp": float(timestamp),
                "recorded_at": datetime.fromtimestamp(float(timestamp), tz=timezone.utc).isoformat(),
                "elapsed_seconds": float(timestamp) - start,
                "metric": spec["name"],
                "title": spec["title"],
                "group": spec["group"],
                "value": value,
                "unit": spec["unit"],
                "labels": labels,
            })
    return rows


def collect(
    base_url: str,
    specs: list[dict[str, str]],
    start: float,
    end: float,
    step: int,
) -> tuple[list[dict[str, Any]], dict[str, Any], list[str]]:
    rows: list[dict[str, Any]] = []
    raw: dict[str, Any] = {}
    warnings: list[str] = []
    for spec in specs:
        try:
            payload = query_range(base_url, spec["query"], start, end, step)
            raw[spec["name"]] = {"spec": spec, "response": payload}
            metric_rows = flatten_result(spec, payload, start)
            if not metric_rows:
                warnings.append(f"{spec['name']}: 시계열 없음")
            rows.extend(metric_rows)
        except (OSError, ValueError, urllib.error.URLError) as error:
            message = f"{spec['name']}: {error}"
            warnings.append(message)
            raw[spec["name"]] = {"spec": spec, "error": str(error)}
    return rows, raw, warnings


def main() -> int:
    args = parse_args()
    if args.end <= args.start:
        raise ValueError("end는 start보다 커야 합니다.")
    if args.step <= 0:
        raise ValueError("step은 1 이상이어야 합니다.")
    specs = json.loads(args.queries.read_text(encoding="utf-8"))
    rows, raw, warnings = collect(args.url, specs, args.start, args.end, args.step)
    origin = args.origin if args.origin is not None else args.start
    for row in rows:
        row["elapsed_seconds"] = row["timestamp"] - origin

    args.output_dir.mkdir(parents=True, exist_ok=True)
    csv_path = args.output_dir / "prometheus-timeseries.csv"
    with csv_path.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=CSV_FIELDS)
        writer.writeheader()
        writer.writerows(rows)
    (args.output_dir / "prometheus-raw.json").write_text(
        json.dumps(raw, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    (args.output_dir / "prometheus-warnings.txt").write_text(
        "\n".join(warnings) + ("\n" if warnings else ""), encoding="utf-8"
    )
    print(csv_path)
    for warning in warnings:
        print(f"warning: {warning}", file=sys.stderr)
    return 1 if args.strict and warnings else 0


if __name__ == "__main__":
    raise SystemExit(main())
