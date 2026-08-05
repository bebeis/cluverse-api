#!/usr/bin/env python3
"""Compare baseline and improvement runs using normalized analysis summaries."""

from __future__ import annotations

import argparse
import base64
import csv
import html
import json
import math
import os
import tempfile
from pathlib import Path
from typing import Any


FIELDS = (
    "label",
    "run_id",
    "profile",
    "max_sustainable_tps",
    "tps_improvement_percent",
    "latency_comparison_tps",
    "read_p95_ms",
    "read_p99_ms",
    "write_p95_ms",
    "write_p99_ms",
    "success_rate_percent",
    "normalization_seconds",
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="여러 devlog-10 실행의 SLO/TPS를 비교합니다.")
    parser.add_argument("--run-dir", action="append", required=True, type=Path)
    parser.add_argument("--output-dir", required=True, type=Path)
    return parser.parse_args()


def common_latency_tps(summaries: list[dict[str, Any]]) -> float:
    common = None
    for summary in summaries:
        targets = {float(row["target_tps"]) for row in summary.get("steps", [])}
        common = targets if common is None else common & targets
    if not common:
        raise ValueError("모든 실행에 공통인 target TPS가 없습니다. 같은 부하 단계로 다시 측정하세요.")
    sustainable = [summary.get("max_sustainable_tps") for summary in summaries]
    limits = [float(value) for value in sustainable if value is not None]
    ceiling = min(limits) if limits else max(common)
    eligible = [target for target in common if target <= ceiling]
    return max(eligible or common)


def read_summary(run_dir: Path) -> dict[str, Any]:
    summary_path = run_dir / "analysis-summary.json"
    if not summary_path.exists():
        raise ValueError(f"analyze.py를 먼저 실행하세요: {summary_path}")
    return json.loads(summary_path.read_text(encoding="utf-8"))


def read_row(summary: dict[str, Any], comparison_tps: float) -> dict[str, Any]:
    matches = [row for row in summary.get("steps", []) if float(row.get("target_tps", -1)) == comparison_tps]
    if not matches:
        raise ValueError(f"{summary['run_id']}에 {comparison_tps} TPS 측정값이 없습니다.")
    step = matches[-1]
    return {
        "label": summary["label"],
        "run_id": summary["run_id"],
        "profile": summary["profile"],
        "max_sustainable_tps": summary.get("max_sustainable_tps"),
        "tps_improvement_percent": None,
        "latency_comparison_tps": comparison_tps,
        "read_p95_ms": step.get("read_p95_ms"),
        "read_p99_ms": step.get("read_p99_ms"),
        "write_p95_ms": step.get("write_p95_ms"),
        "write_p99_ms": step.get("write_p99_ms"),
        "success_rate_percent": step.get("success_rate_percent"),
        "normalization_seconds": summary.get("normalization_seconds"),
    }


def percentage(value: float | None, baseline: float | None) -> float | None:
    if value is None or baseline is None or baseline == 0:
        return None
    return (value / baseline - 1) * 100


def number(value: Any, digits: int = 1, suffix: str = "") -> str:
    if value is None or not isinstance(value, (int, float)) or not math.isfinite(value):
        return "N/A"
    return f"{value:,.{digits}f}{suffix}"


def render_chart(rows: list[dict[str, Any]], comparison_tps: float, output: Path) -> None:
    try:
        os.environ.setdefault("MPLCONFIGDIR", str(Path(tempfile.gettempdir()) / "cluverse-matplotlib"))
        import matplotlib
        matplotlib.use("Agg")
        import matplotlib.pyplot as plt
    except ImportError as error:
        raise RuntimeError(
            "matplotlib이 없습니다. python3 -m pip install -r script/spike-traffic/requirements.txt"
        ) from error

    plt.style.use("seaborn-v0_8-whitegrid")
    labels = [row["label"] for row in rows]
    x = list(range(len(rows)))
    blue, orange, navy = "#2563EB", "#F59E0B", "#172554"
    fig, axes = plt.subplots(1, 2, figsize=(16, 7), constrained_layout=True)
    tps = [row["max_sustainable_tps"] or 0 for row in rows]
    bars = axes[0].bar(x, tps, color=blue, width=0.62)
    axes[0].bar_label(bars, fmt="%.0f TPS", padding=5, fontweight="bold")
    axes[0].set_xticks(x, labels)
    axes[0].set_ylabel("SLO sustainable TPS")
    axes[0].set_title("Sustainable throughput", fontweight="bold")

    width = 0.34
    axes[1].bar([value - width / 2 for value in x], [row["read_p99_ms"] or 0 for row in rows], width, color=blue, label="Read p99")
    axes[1].bar([value + width / 2 for value in x], [row["write_p99_ms"] or 0 for row in rows], width, color=orange, label="Write p99")
    axes[1].set_xticks(x, labels)
    axes[1].set_ylabel("latency (ms)")
    axes[1].set_title(f"Tail latency at fixed {comparison_tps:.0f} TPS", fontweight="bold")
    axes[1].legend()
    fig.suptitle("Cluverse spike traffic — measured improvements", fontsize=19, fontweight="bold", color=navy)
    fig.savefig(output, dpi=150, facecolor="white")
    plt.close(fig)


def render_html(rows: list[dict[str, Any]], chart: Path, output: Path) -> None:
    image = base64.b64encode(chart.read_bytes()).decode("ascii")
    table_rows = "".join(
        "<tr>"
        f"<td>{html.escape(str(row['label']))}</td>"
        f"<td>{number(row['max_sustainable_tps'], 0)}</td>"
        f"<td>{number(row['tps_improvement_percent'], 1, '%')}</td>"
        f"<td>{number(row['latency_comparison_tps'], 0)}</td>"
        f"<td>{number(row['read_p95_ms'])} / {number(row['read_p99_ms'])}</td>"
        f"<td>{number(row['write_p95_ms'])} / {number(row['write_p99_ms'])}</td>"
        f"<td>{number(row['success_rate_percent'], 3, '%')}</td>"
        f"<td>{number(row['normalization_seconds'], 0, 's')}</td>"
        "</tr>"
        for row in rows
    )
    output.write_text(f"""<!doctype html><html lang="ko"><head><meta charset="utf-8"><title>Cluverse measured improvements</title>
<style>body{{margin:0;background:#f8fafc;font-family:Inter,Pretendard,-apple-system,sans-serif;color:#0f172a}}main{{max-width:1440px;margin:auto;padding:48px}}h1{{color:#172554}}section{{background:#fff;border:1px solid #dbe3ef;border-radius:18px;padding:26px;margin:22px 0;box-shadow:0 8px 25px #0f172a0b}}img{{width:100%}}table{{width:100%;border-collapse:collapse}}th,td{{padding:13px;border-bottom:1px solid #dbe3ef;text-align:right}}th:first-child,td:first-child{{text-align:left}}th{{color:#64748b;font-size:13px}}</style>
</head><body><main><h1>Cluverse Spike Traffic Comparison</h1><section><img src="data:image/png;base64,{image}"></section><section><table><thead><tr><th>Label</th><th>Sustainable TPS</th><th>Improvement</th><th>Latency comparison TPS</th><th>Read p95 / p99</th><th>Write p95 / p99</th><th>Success</th><th>Normalization</th></tr></thead><tbody>{table_rows}</tbody></table></section></main></body></html>""", encoding="utf-8")


def main() -> int:
    args = parse_args()
    if len(args.run_dir) < 2:
        raise ValueError("비교에는 run-dir이 2개 이상 필요합니다.")
    summaries = [read_summary(path) for path in args.run_dir]
    comparison_tps = common_latency_tps(summaries)
    rows = [read_row(summary, comparison_tps) for summary in summaries]
    baseline = rows[0]["max_sustainable_tps"]
    for row in rows:
        row["tps_improvement_percent"] = percentage(row["max_sustainable_tps"], baseline)

    args.output_dir.mkdir(parents=True, exist_ok=True)
    csv_path = args.output_dir / "comparison.csv"
    with csv_path.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=FIELDS)
        writer.writeheader()
        writer.writerows(rows)
    chart = args.output_dir / "comparison.png"
    render_chart(rows, comparison_tps, chart)
    render_html(rows, chart, args.output_dir / "comparison.html")
    print(args.output_dir / "comparison.html")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
