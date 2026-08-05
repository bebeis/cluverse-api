#!/usr/bin/env python3
"""Turn MySQL EXPLAIN ANALYZE text into node CSV, summary JSON and capture HTML."""

from __future__ import annotations

import argparse
import csv
import html
import json
import re
from pathlib import Path
from typing import Any


ACTUAL_PATTERN = re.compile(
    r"actual time=([0-9.]+)\.\.([0-9.]+) rows=([0-9.]+) loops=([0-9.]+)", re.IGNORECASE
)
NODE_FIELDS = (
    "node_index",
    "operator",
    "actual_start_ms",
    "actual_end_ms",
    "rows_per_loop",
    "loops",
    "output_rows",
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="MySQL EXPLAIN ANALYZE 실행 계획을 정규화합니다.")
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--label", default="query")
    parser.add_argument("--output-dir", required=True, type=Path)
    return parser.parse_args()


def parse_nodes(text: str) -> list[dict[str, Any]]:
    nodes: list[dict[str, Any]] = []
    for line in text.splitlines():
        match = ACTUAL_PATTERN.search(line)
        if not match:
            continue
        operator = line[: match.start()].strip()
        operator = re.sub(r"^[\s|`+\\-]*>\s*", "", operator)
        operator = re.sub(r"\s*\(cost=.*$", "", operator).strip()
        start, end, rows, loops = (float(value) for value in match.groups())
        nodes.append({
            "node_index": len(nodes) + 1,
            "operator": operator or "unknown",
            "actual_start_ms": start,
            "actual_end_ms": end,
            "rows_per_loop": rows,
            "loops": loops,
            "output_rows": rows * loops,
        })
    if not nodes:
        raise ValueError("actual time/rows/loops를 포함한 실행 계획 노드를 찾지 못했습니다.")
    return nodes


def summarize(text: str, nodes: list[dict[str, Any]], label: str) -> dict[str, Any]:
    lowered = text.lower()
    root = nodes[0]
    largest = max(nodes, key=lambda node: node["output_rows"])
    return {
        "label": label,
        "root_actual_end_ms": root["actual_end_ms"],
        "root_output_rows": root["output_rows"],
        "plan_node_count": len(nodes),
        "largest_output_node": largest["operator"],
        "largest_output_rows": largest["output_rows"],
        "uses_sort": "sort:" in lowered or "filesort" in lowered,
        "uses_temporary_or_materialize": "temporary" in lowered or "materialize" in lowered,
        "uses_table_scan": "table scan" in lowered,
        "uses_index": "index scan" in lowered or "index lookup" in lowered or "index range scan" in lowered,
    }


def render_html(path: Path, label: str, text: str, summary: dict[str, Any], nodes: list[dict[str, Any]]) -> None:
    flags = "".join(
        f"<span class={'bad' if enabled and name != 'Index access' else 'good'}>{html.escape(name)}: {'yes' if enabled else 'no'}</span>"
        for name, enabled in (
            ("Sort", summary["uses_sort"]),
            ("Temporary / materialize", summary["uses_temporary_or_materialize"]),
            ("Table scan", summary["uses_table_scan"]),
            ("Index access", summary["uses_index"]),
        )
    )
    rows = "".join(
        "<tr>"
        f"<td>{node['node_index']}</td><td>{html.escape(node['operator'])}</td>"
        f"<td>{node['actual_end_ms']:.3f}</td><td>{node['rows_per_loop']:.0f}</td>"
        f"<td>{node['loops']:.0f}</td><td>{node['output_rows']:.0f}</td></tr>"
        for node in nodes
    )
    path.write_text(f"""<!doctype html><html lang="ko"><head><meta charset="utf-8"><title>EXPLAIN — {html.escape(label)}</title>
<style>body{{margin:0;background:#f8fafc;font-family:Inter,Pretendard,-apple-system,sans-serif;color:#0f172a}}main{{max-width:1400px;margin:auto;padding:46px}}h1{{color:#172554}}.cards{{display:grid;grid-template-columns:repeat(3,1fr);gap:14px}}.card,section{{background:white;border:1px solid #dbe3ef;border-radius:16px;padding:22px;margin:18px 0;box-shadow:0 8px 24px #0f172a0b}}.card span{{color:#64748b;display:block;font-size:13px}}.card strong{{font-size:25px;color:#172554}}.flags{{display:flex;gap:10px;flex-wrap:wrap}}.flags span{{padding:8px 12px;border-radius:99px;font-weight:700}}.good{{background:#d1fae5;color:#047857}}.bad{{background:#fee2e2;color:#b91c1c}}table{{width:100%;border-collapse:collapse}}th,td{{padding:11px;border-bottom:1px solid #e2e8f0;text-align:right}}th:nth-child(2),td:nth-child(2){{text-align:left}}pre{{white-space:pre-wrap;background:#0f172a;color:#e2e8f0;border-radius:12px;padding:20px;font-size:13px;line-height:1.55;overflow:auto}}</style>
</head><body><main><h1>EXPLAIN ANALYZE · {html.escape(label)}</h1><div class="cards"><div class="card"><span>Root actual time</span><strong>{summary['root_actual_end_ms']:.3f} ms</strong></div><div class="card"><span>Plan nodes</span><strong>{summary['plan_node_count']}</strong></div><div class="card"><span>Largest node output</span><strong>{summary['largest_output_rows']:.0f}</strong></div></div><section class="flags">{flags}</section><section><h2>Plan nodes</h2><table><thead><tr><th>#</th><th>Operator</th><th>End ms</th><th>Rows / loop</th><th>Loops</th><th>Output rows</th></tr></thead><tbody>{rows}</tbody></table></section><section><h2>Raw plan</h2><pre>{html.escape(text)}</pre></section></main></body></html>""", encoding="utf-8")


def main() -> int:
    args = parse_args()
    text = args.input.read_text(encoding="utf-8")
    nodes = parse_nodes(text)
    summary = summarize(text, nodes, args.label)
    args.output_dir.mkdir(parents=True, exist_ok=True)
    with (args.output_dir / "explain-nodes.csv").open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=NODE_FIELDS)
        writer.writeheader()
        writer.writerows(nodes)
    (args.output_dir / "explain-summary.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    render_html(args.output_dir / "explain-report.html", args.label, text, summary, nodes)
    print(args.output_dir / "explain-report.html")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
