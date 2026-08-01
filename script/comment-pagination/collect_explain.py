#!/usr/bin/env python3
"""Extract comparable comment page nodes from MySQL EXPLAIN ANALYZE output."""

from __future__ import annotations

import argparse
import csv
import re
from dataclasses import asdict, dataclass
from datetime import datetime
from pathlib import Path


CSV_FIELDS = (
    "experiment",
    "run_id",
    "recorded_at",
    "version",
    "scenario",
    "source",
    "metric",
    "stat",
    "value",
    "unit",
    "input_file",
)


@dataclass(frozen=True)
class ExplainMeasurement:
    experiment: str
    run_id: str
    recorded_at: str
    version: str
    scenario: str
    source: str
    metric: str
    stat: str
    value: float
    unit: str
    input_file: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="댓글 페이지 EXPLAIN ANALYZE를 정규화 CSV로 변환합니다.")
    parser.add_argument("--before", required=True, type=Path, help="재귀 CTE EXPLAIN 출력")
    parser.add_argument("--after", required=True, type=Path, help="path 인덱스 EXPLAIN 출력")
    parser.add_argument("--comments", required=True, type=int)
    parser.add_argument("--tree-shape", default="mixed")
    parser.add_argument("--cursor-position", default="first")
    parser.add_argument("--output", required=True, type=Path)
    return parser.parse_args()


def extract_node(text: str, version: str) -> tuple[float, float]:
    if version == "v1":
        node = r"Table scan on comment_tree"
    elif version == "v2":
        node = r"Index (?:lookup|range scan) on comment using idx_comment_post_path"
    else:
        raise ValueError(f"지원하지 않는 버전입니다: {version}")
    match = re.search(
        node + r".*?actual time=[^)]*?rows=([0-9.]+) loops=([0-9.]+)",
        text,
        flags=re.DOTALL,
    )
    if not match:
        raise ValueError(f"{version} 비교 노드의 actual rows/loops를 찾지 못했습니다.")
    return float(match.group(1)), float(match.group(2))


def measurements(path: Path, version: str, scenario: str, recorded_at: str) -> list[ExplainMeasurement]:
    text = path.read_text(encoding="utf-8")
    actual_rows, actual_loops = extract_node(text, version)
    values = (
        ("actual_rows", actual_rows, "count"),
        ("actual_loops", actual_loops, "count"),
        ("uses_sort", float("Sort:" in text), "boolean"),
        ("uses_materialization", float("Materialize" in text), "boolean"),
    )
    return [
        ExplainMeasurement(
            experiment="comment-pagination",
            run_id=path.stem,
            recorded_at=recorded_at,
            version=version,
            scenario=scenario,
            source="mysql-explain",
            metric=metric,
            stat="point",
            value=value,
            unit=unit,
            input_file=str(path),
        )
        for metric, value, unit in values
    ]


def main() -> int:
    args = parse_args()
    if args.comments <= 0:
        raise ValueError("comments는 1 이상이어야 합니다.")
    scenario = ",".join((
        f"comments={args.comments}",
        f"tree_shape={args.tree_shape}",
        f"cursor_position={args.cursor_position}",
    ))
    recorded_at = datetime.now().astimezone().isoformat()
    rows = [
        *measurements(args.before, "v1", scenario, recorded_at),
        *measurements(args.after, "v2", scenario, recorded_at),
    ]
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=CSV_FIELDS)
        writer.writeheader()
        writer.writerows(asdict(row) for row in rows)
    print(args.output)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
