#!/usr/bin/env python3
"""Normalize MySQL EXPLAIN ANALYZE evidence for recent-commented-post queries."""

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
    parser = argparse.ArgumentParser(description="홈 최근 댓글 글 EXPLAIN ANALYZE를 CSV로 변환합니다.")
    parser.add_argument("--v1", required=True, type=Path, help="일반 GROUP BY 실행 계획")
    parser.add_argument("--v2", required=True, type=Path, help="Loose Index Scan 실행 계획")
    parser.add_argument("--v3", required=True, type=Path, help="활동 투영 테이블 실행 계획")
    parser.add_argument("--comments", required=True, type=int)
    parser.add_argument("--commented-posts", required=True, type=int)
    parser.add_argument("--hot-comment-percent", required=True, type=int)
    parser.add_argument("--output", required=True, type=Path)
    return parser.parse_args()


def extract_node(text: str, version: str) -> tuple[float, float]:
    patterns = {
        "v1": (
            r"(?:Covering\s+)?(?:Index|Table) (?:scan|lookup|range scan) on c(?:\s+using\s+\S+)?",
            r"(?:Covering\s+)?(?:Index|Table) (?:scan|lookup|range scan) on comment(?:\s+using\s+\S+)?",
        ),
        "v2": (
            r"(?:Covering\s+)?index skip scan for grouping on c(?:\s+using\s+idx_comment_post_visible_created)?",
            r"(?:Covering\s+)?index skip scan for grouping on comment(?:\s+using\s+idx_comment_post_visible_created)?",
        ),
        "v3": (
            r"(?:Covering\s+)?Index (?:scan|lookup|range scan) on activity(?:\s+using\s+idx_post_comment_activity_latest)?",
            r"(?:Covering\s+)?Index (?:scan|lookup|range scan) on post_comment_activity(?:\s+using\s+idx_post_comment_activity_latest)?",
        ),
    }
    if version not in patterns:
        raise ValueError(f"지원하지 않는 버전입니다: {version}")
    for node in patterns[version]:
        match = re.search(
            node + r".*?actual time=[^)]*?rows=([0-9.]+) loops=([0-9.]+)",
            text,
            flags=re.IGNORECASE | re.DOTALL,
        )
        if match:
            return float(match.group(1)), float(match.group(2))
    raise ValueError(f"{version} 비교 노드의 actual rows/loops를 찾지 못했습니다.")


def measurements(path: Path, version: str, scenario: str, recorded_at: str) -> list[ExplainMeasurement]:
    text = path.read_text(encoding="utf-8")
    actual_rows_per_loop, actual_loops = extract_node(text, version)
    lowered = text.lower()
    values = (
        ("actual_rows", actual_rows_per_loop * actual_loops, "count"),
        ("actual_rows_per_loop", actual_rows_per_loop, "count"),
        ("actual_loops", actual_loops, "count"),
        ("uses_sort", float("sort:" in lowered or "filesort" in lowered), "boolean"),
        ("uses_temporary", float("temporary" in lowered or "materialize" in lowered), "boolean"),
        (
            "uses_loose_index",
            float("using index for group-by" in lowered or "index skip scan for grouping" in lowered),
            "boolean",
        ),
    )
    return [
        ExplainMeasurement(
            experiment="home-feed",
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
    if args.comments <= 0 or args.commented_posts <= 0:
        raise ValueError("comments와 commented-posts는 1 이상이어야 합니다.")
    if not 0 <= args.hot_comment_percent <= 100:
        raise ValueError("hot-comment-percent는 0 이상 100 이하여야 합니다.")
    scenario = ",".join((
        f"comments={args.comments}",
        f"commented_posts={args.commented_posts}",
        f"hot_comment_percent={args.hot_comment_percent}",
    ))
    recorded_at = datetime.now().astimezone().isoformat()
    rows = [
        *measurements(args.v1, "v1", scenario, recorded_at),
        *measurements(args.v2, "v2", scenario, recorded_at),
        *measurements(args.v3, "v3", scenario, recorded_at),
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
