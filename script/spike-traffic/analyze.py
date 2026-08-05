#!/usr/bin/env python3
"""Normalize k6/Prometheus evidence and render devlog-10 charts and an HTML capture report."""

from __future__ import annotations

import argparse
import base64
import csv
import html
import json
import math
import os
import statistics
import tempfile
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable, Sequence


K6_FIELDS = (
    "timestamp",
    "recorded_at",
    "elapsed_seconds",
    "target_tps",
    "completed_rps",
    "read_p95_ms",
    "read_p99_ms",
    "write_p95_ms",
    "write_p99_ms",
    "success_rate_percent",
    "dropped_iterations",
    "phase",
)

STEP_FIELDS = (
    "label",
    "target_tps",
    "completed_rps",
    "read_p95_ms",
    "read_p99_ms",
    "write_p95_ms",
    "write_p99_ms",
    "success_rate_percent",
    "dropped_iterations",
    "slo_pass",
)


def parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="devlog-10 k6/Prometheus 결과를 CSV·PNG·HTML로 만듭니다.")
    parser.add_argument("--run-dir", required=True, type=Path)
    parser.add_argument("--bucket-seconds", default=5, type=int)
    parser.add_argument("--skip-charts", action="store_true")
    return parser.parse_args(argv)


def quantile(values: Iterable[float], probability: float) -> float:
    ordered = sorted(values)
    if not ordered:
        return math.nan
    if len(ordered) == 1:
        return ordered[0]
    position = (len(ordered) - 1) * probability
    lower = math.floor(position)
    upper = math.ceil(position)
    if lower == upper:
        return ordered[lower]
    fraction = position - lower
    return ordered[lower] * (1 - fraction) + ordered[upper] * fraction


def read_metadata(run_dir: Path) -> dict[str, Any]:
    path = run_dir / "metadata.json"
    if not path.exists():
        raise ValueError(f"metadata.json이 없습니다: {path}")
    return json.loads(path.read_text(encoding="utf-8"))


def metric_samples(path: Path) -> dict[str, list[tuple[float, float]]]:
    samples: dict[str, list[tuple[float, float]]] = defaultdict(list)
    with path.open(encoding="utf-8", newline="") as file:
        for row in csv.DictReader(file):
            try:
                samples[row["metric_name"]].append((float(row["timestamp"]), float(row["metric_value"])))
            except (KeyError, TypeError, ValueError):
                continue
    return samples


def phase_and_target(metadata: dict[str, Any], elapsed: float) -> tuple[str, float]:
    profile = metadata["profile"]
    load = metadata["load"]
    if profile == "capacity":
        rates = [float(value) for value in load["capacity_rates"]]
        step_seconds = float(load["step_duration_seconds"])
        index = min(int(max(elapsed, 0) // step_seconds), len(rates) - 1)
        return f"capacity-{int(rates[index])}", rates[index]
    if profile == "smoke":
        return "smoke", float(load["smoke_rate"])

    normal = float(load["normal_rate"])
    spike = float(load["spike_rate"])
    baseline_end = float(load["baseline_seconds"])
    ramp_end = baseline_end + float(load["ramp_seconds"])
    spike_end = ramp_end + float(load["spike_seconds"])
    recovery_start = spike_end + 1
    if elapsed < baseline_end:
        return "baseline", normal
    if elapsed < ramp_end:
        ratio = (elapsed - baseline_end) / max(float(load["ramp_seconds"]), 1)
        return "ramp", normal + (spike - normal) * ratio
    if elapsed < spike_end:
        return "spike", spike
    if elapsed < recovery_start:
        return "ramp-down", normal
    return "recovery", normal


def values_in_bucket(
    samples: dict[str, list[tuple[float, float]]],
    metric: str,
    start: float,
    end: float,
) -> list[float]:
    return [value for timestamp, value in samples.get(metric, []) if start <= timestamp < end]


def build_timeseries(
    samples: dict[str, list[tuple[float, float]]],
    metadata: dict[str, Any],
    bucket_seconds: int,
) -> list[dict[str, Any]]:
    start = float(metadata["start_epoch"])
    end = float(metadata["end_epoch"])
    rows: list[dict[str, Any]] = []
    bucket_start = start
    while bucket_start < end:
        bucket_end = min(bucket_start + bucket_seconds, end)
        elapsed = bucket_start - start
        duration = max(bucket_end - bucket_start, 1)
        reads = values_in_bucket(samples, "core_read_duration", bucket_start, bucket_end)
        writes = values_in_bucket(samples, "core_write_duration", bucket_start, bucket_end)
        successes = values_in_bucket(samples, "core_request_success", bucket_start, bucket_end)
        requests = values_in_bucket(samples, "http_reqs", bucket_start, bucket_end)
        dropped = values_in_bucket(samples, "dropped_iterations", bucket_start, bucket_end)
        phase, target_tps = phase_and_target(metadata, elapsed + duration / 2)
        rows.append({
            "timestamp": bucket_start,
            "recorded_at": datetime.fromtimestamp(bucket_start, tz=timezone.utc).isoformat(),
            "elapsed_seconds": elapsed,
            "target_tps": target_tps,
            "completed_rps": sum(requests) / duration,
            "read_p95_ms": quantile(reads, 0.95),
            "read_p99_ms": quantile(reads, 0.99),
            "write_p95_ms": quantile(writes, 0.95),
            "write_p99_ms": quantile(writes, 0.99),
            "success_rate_percent": statistics.fmean(successes) * 100 if successes else math.nan,
            "dropped_iterations": sum(dropped),
            "phase": phase,
        })
        bucket_start += bucket_seconds
    return rows


def segment_samples(
    samples: dict[str, list[tuple[float, float]]], metric: str, start: float, end: float
) -> list[float]:
    return [value for timestamp, value in samples.get(metric, []) if start <= timestamp < end]


def summarize_segment(
    label: str,
    target_tps: float,
    start: float,
    end: float,
    samples: dict[str, list[tuple[float, float]]],
    slo: dict[str, float],
) -> dict[str, Any]:
    reads = segment_samples(samples, "core_read_duration", start, end)
    writes = segment_samples(samples, "core_write_duration", start, end)
    successes = segment_samples(samples, "core_request_success", start, end)
    requests = segment_samples(samples, "http_reqs", start, end)
    dropped = segment_samples(samples, "dropped_iterations", start, end)
    duration = max(end - start, 1)
    row = {
        "label": label,
        "target_tps": target_tps,
        "completed_rps": sum(requests) / duration,
        "read_p95_ms": quantile(reads, 0.95),
        "read_p99_ms": quantile(reads, 0.99),
        "write_p95_ms": quantile(writes, 0.95),
        "write_p99_ms": quantile(writes, 0.99),
        "success_rate_percent": statistics.fmean(successes) * 100 if successes else math.nan,
        "dropped_iterations": sum(dropped),
    }
    enough_throughput = row["completed_rps"] >= target_tps * 0.98
    success_pass = row["success_rate_percent"] >= slo["success_rate"] * 100
    read_pass = row["read_p95_ms"] < slo["read_p95_ms"] and row["read_p99_ms"] < slo["read_p99_ms"]
    write_pass = (
        not writes
        or row["write_p95_ms"] < slo["write_p95_ms"]
        and row["write_p99_ms"] < slo["write_p99_ms"]
    )
    row["slo_pass"] = enough_throughput and success_pass and read_pass and write_pass and row["dropped_iterations"] == 0
    return row


def build_step_summary(
    samples: dict[str, list[tuple[float, float]]], metadata: dict[str, Any]
) -> list[dict[str, Any]]:
    start = float(metadata["start_epoch"])
    end = float(metadata["end_epoch"])
    load = metadata["load"]
    slo = metadata["slo"]
    if metadata["profile"] == "capacity":
        rows = []
        step_seconds = float(load["step_duration_seconds"])
        settle = min(float(load.get("settle_seconds", 15)), step_seconds * 0.25)
        for index, rate in enumerate(load["capacity_rates"]):
            segment_start = start + index * step_seconds + settle
            segment_end = min(start + (index + 1) * step_seconds, end)
            rows.append(summarize_segment(f"{rate} TPS", float(rate), segment_start, segment_end, samples, slo))
        return rows
    if metadata["profile"] == "smoke":
        return [summarize_segment("smoke", float(load["smoke_rate"]), start, end, samples, slo)]

    baseline_end = start + float(load["baseline_seconds"])
    ramp_end = baseline_end + float(load["ramp_seconds"])
    spike_end = ramp_end + float(load["spike_seconds"])
    recovery_start = spike_end + 1
    return [
        summarize_segment("baseline", float(load["normal_rate"]), start, baseline_end, samples, slo),
        summarize_segment("spike", float(load["spike_rate"]), ramp_end, spike_end, samples, slo),
        summarize_segment("recovery", float(load["normal_rate"]), recovery_start, end, samples, slo),
    ]


def normalization_seconds(timeseries: list[dict[str, Any]], metadata: dict[str, Any], bucket_seconds: int) -> float | None:
    if metadata["profile"] != "spike":
        return None
    load = metadata["load"]
    recovery_start = (
        float(load["baseline_seconds"])
        + float(load["ramp_seconds"])
        + float(load["spike_seconds"])
        + 1
    )
    required = max(math.ceil(float(load.get("normalization_window_seconds", 30)) / bucket_seconds), 1)
    slo = metadata["slo"]
    allowed_failure = (1 - float(slo["success_rate"])) * 100
    consecutive = 0
    for row in timeseries:
        if row["elapsed_seconds"] < recovery_start:
            continue
        read_ok = math.isfinite(row["read_p99_ms"]) and row["read_p99_ms"] < slo["read_p99_ms"]
        write_ok = not math.isfinite(row["write_p99_ms"]) or row["write_p99_ms"] < slo["write_p99_ms"]
        success_ok = (
            math.isfinite(row["success_rate_percent"])
            and 100 - row["success_rate_percent"] <= allowed_failure + 1e-9
        )
        dropped_ok = row["dropped_iterations"] == 0
        consecutive = consecutive + 1 if read_ok and write_ok and success_ok and dropped_ok else 0
        if consecutive >= required:
            first_stable_elapsed = row["elapsed_seconds"] - (required - 1) * bucket_seconds
            return max(first_stable_elapsed - recovery_start, 0)
    return None


def write_csv(path: Path, fields: Sequence[str], rows: Iterable[dict[str, Any]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)


def finite(values: Iterable[float]) -> list[float]:
    return [value for value in values if isinstance(value, (int, float)) and math.isfinite(value)]


def load_prometheus(run_dir: Path) -> list[dict[str, Any]]:
    path = run_dir / "prometheus-timeseries.csv"
    if not path.exists():
        return []
    rows = []
    with path.open(encoding="utf-8", newline="") as file:
        for row in csv.DictReader(file):
            try:
                rows.append({**row, "elapsed_seconds": float(row["elapsed_seconds"]), "value": float(row["value"])})
            except (TypeError, ValueError):
                continue
    return rows


def render_charts(
    run_dir: Path,
    metadata: dict[str, Any],
    timeseries: list[dict[str, Any]],
    steps: list[dict[str, Any]],
    prometheus: list[dict[str, Any]],
) -> list[Path]:
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
    navy, blue, orange, red, green, gray = "#172554", "#2563EB", "#F59E0B", "#DC2626", "#059669", "#64748B"
    x = [row["elapsed_seconds"] for row in timeseries]
    outputs: list[Path] = []

    fig, axes = plt.subplots(3, 1, figsize=(16, 10), sharex=True, constrained_layout=True)
    axes[0].plot(x, [row["target_tps"] for row in timeseries], color=gray, linestyle="--", label="Offered TPS")
    axes[0].plot(x, [row["completed_rps"] for row in timeseries], color=blue, linewidth=2, label="Completed RPS")
    axes[0].set_ylabel("requests / second")
    axes[0].legend(loc="upper left", ncol=2)

    axes[1].plot(x, [row["read_p99_ms"] for row in timeseries], color=blue, label="Read p99")
    axes[1].plot(x, [row["write_p99_ms"] for row in timeseries], color=orange, label="Write p99")
    axes[1].axhline(metadata["slo"]["read_p99_ms"], color=blue, linestyle=":", alpha=0.7, label="Read SLO")
    axes[1].axhline(metadata["slo"]["write_p99_ms"], color=orange, linestyle=":", alpha=0.7, label="Write SLO")
    axes[1].set_ylabel("latency (ms)")
    axes[1].legend(loc="upper left", ncol=4)

    axes[2].plot(x, [100 - row["success_rate_percent"] for row in timeseries], color=red, label="Failure %")
    axes[2].axhline((1 - metadata["slo"]["success_rate"]) * 100, color=red, linestyle=":", label="Failure SLO")
    axes[2].set_ylabel("failure percent", color=red)
    dropped_axis = axes[2].twinx()
    dropped_axis.bar(x, [row["dropped_iterations"] for row in timeseries], width=max(1, x[1] - x[0] if len(x) > 1 else 1), color=navy, alpha=0.25, label="Dropped iterations")
    dropped_axis.set_ylabel("dropped iterations", color=navy)
    axes[2].set_xlabel("elapsed seconds")
    handles, labels = axes[2].get_legend_handles_labels()
    dropped_handles, dropped_labels = dropped_axis.get_legend_handles_labels()
    axes[2].legend(handles + dropped_handles, labels + dropped_labels, loc="upper left", ncol=3)
    if metadata["profile"] == "spike":
        load = metadata["load"]
        baseline_end = float(load["baseline_seconds"])
        ramp_end = baseline_end + float(load["ramp_seconds"])
        spike_end = ramp_end + float(load["spike_seconds"])
        recovery_start = spike_end + 1
        for axis in axes:
            axis.axvspan(baseline_end, ramp_end, color=orange, alpha=0.08)
            axis.axvspan(ramp_end, spike_end, color=red, alpha=0.06)
            axis.axvspan(recovery_start, max(x, default=recovery_start), color=green, alpha=0.05)
    overview_title = "Spike traffic overview" if metadata["profile"] == "spike" else "Capacity test overview"
    fig.suptitle(f"{overview_title} — {metadata['label']}", fontsize=18, fontweight="bold", color=navy)
    overview = run_dir / "spike-overview.png"
    fig.savefig(overview, dpi=150, facecolor="white")
    plt.close(fig)
    outputs.append(overview)

    if metadata["profile"] == "capacity" and steps:
        fig, left = plt.subplots(figsize=(14, 7), constrained_layout=True)
        targets = [row["target_tps"] for row in steps]
        left.plot(targets, [row["completed_rps"] for row in steps], marker="o", color=blue, linewidth=2.5, label="Completed RPS")
        left.plot(targets, targets, color=gray, linestyle="--", label="Offered TPS")
        left.set_xlabel("target TPS")
        left.set_ylabel("completed requests / second", color=blue)
        right = left.twinx()
        right.plot(targets, [row["read_p99_ms"] for row in steps], marker="s", color=orange, label="Read p99")
        right.axhline(metadata["slo"]["read_p99_ms"], color=red, linestyle=":", label="Read p99 SLO")
        right.set_ylabel("latency (ms)", color=orange)
        handles_left, labels_left = left.get_legend_handles_labels()
        handles_right, labels_right = right.get_legend_handles_labels()
        left.legend(handles_left + handles_right, labels_left + labels_right, loc="upper left", ncol=2)
        left.set_title("Capacity curve and SLO breakpoint", fontsize=18, fontweight="bold", color=navy)
        capacity = run_dir / "capacity-curve.png"
        fig.savefig(capacity, dpi=150, facecolor="white")
        plt.close(fig)
        outputs.append(capacity)

    if prometheus:
        panels = (
            ("Application concurrency", ("hikari_active", "hikari_pending", "tomcat_threads_busy")),
            ("Application wait (ms)", ("hikari_acquire_avg_ms", "hikari_usage_avg_ms")),
            ("CPU pressure (%)", ("app_cpu_percent", "mysql_cpu_percent")),
            ("Storage work", ("mysql_qps", "mysql_row_lock_waits_per_second", "redis_commands_per_second")),
        )
        fig, axes = plt.subplots(2, 2, figsize=(16, 10), constrained_layout=True)
        colors = (blue, orange, red, green, navy, gray)
        for axis, (title, metric_names) in zip(axes.flat, panels):
            for index, metric_name in enumerate(metric_names):
                points = [row for row in prometheus if row["metric"] == metric_name]
                if points:
                    axis.plot(
                        [row["elapsed_seconds"] for row in points],
                        [row["value"] for row in points],
                        label=points[0]["title"],
                        color=colors[index],
                        linewidth=2,
                    )
            axis.set_title(title, fontweight="bold")
            axis.set_xlabel("elapsed seconds")
            if axis.lines:
                axis.legend(loc="upper left")
        fig.suptitle(f"Bottleneck signals — {metadata['label']}", fontsize=18, fontweight="bold", color=navy)
        bottleneck = run_dir / "bottleneck-signals.png"
        fig.savefig(bottleneck, dpi=150, facecolor="white")
        plt.close(fig)
        outputs.append(bottleneck)
    return outputs


def format_number(value: Any, digits: int = 1, suffix: str = "") -> str:
    if value is None or not isinstance(value, (int, float)) or not math.isfinite(value):
        return "N/A"
    return f"{value:,.{digits}f}{suffix}"


def image_data(path: Path) -> str:
    encoded = base64.b64encode(path.read_bytes()).decode("ascii")
    return f"data:image/png;base64,{encoded}"


def render_html(
    run_dir: Path,
    metadata: dict[str, Any],
    steps: list[dict[str, Any]],
    normalization: float | None,
    charts: list[Path],
) -> Path:
    passing = [row for row in steps if row["slo_pass"]]
    max_tps = max((row["target_tps"] for row in passing), default=math.nan)
    peak = max(steps, key=lambda row: row["target_tps"]) if steps else {}
    cards = (
        ("SLO sustainable TPS", format_number(max_tps, 0, " TPS")),
        ("Read p99", format_number(peak.get("read_p99_ms"), 1, " ms")),
        ("Write p99", format_number(peak.get("write_p99_ms"), 1, " ms")),
        ("Success rate", format_number(peak.get("success_rate_percent"), 3, "%")),
        ("Normalization", format_number(normalization, 0, " s")),
    )
    rows_html = "".join(
        "<tr>"
        f"<td>{html.escape(str(row['label']))}</td>"
        f"<td>{format_number(row['target_tps'], 0)}</td>"
        f"<td>{format_number(row['completed_rps'], 1)}</td>"
        f"<td>{format_number(row['read_p95_ms'])} / {format_number(row['read_p99_ms'])}</td>"
        f"<td>{format_number(row['write_p95_ms'])} / {format_number(row['write_p99_ms'])}</td>"
        f"<td>{format_number(row['success_rate_percent'], 3, '%')}</td>"
        f"<td class={'pass' if row['slo_pass'] else 'fail'}>{'PASS' if row['slo_pass'] else 'FAIL'}</td>"
        "</tr>"
        for row in steps
    )
    images = "".join(
        f"<section><h2>{html.escape(path.stem.replace('-', ' ').title())}</h2><img src=\"{image_data(path)}\" alt=\"{html.escape(path.stem)}\"></section>"
        for path in charts
    )
    card_html = "".join(
        f"<div class=\"card\"><span>{html.escape(title)}</span><strong>{html.escape(value)}</strong></div>"
        for title, value in cards
    )
    document = f"""<!doctype html>
<html lang="ko"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Cluverse spike traffic — {html.escape(metadata['label'])}</title>
<style>
:root{{--ink:#172554;--blue:#2563eb;--muted:#64748b;--line:#dbe3ef;--bg:#f8fafc;--green:#047857;--red:#b91c1c}}
*{{box-sizing:border-box}} body{{margin:0;background:var(--bg);color:#0f172a;font-family:Inter,Pretendard,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif}}
main{{max-width:1440px;margin:0 auto;padding:48px}} header{{background:linear-gradient(135deg,#172554,#1d4ed8);color:white;padding:38px 42px;border-radius:22px;box-shadow:0 18px 45px #1e3a8a25}}
h1{{margin:0 0 10px;font-size:36px}} header p{{margin:0;color:#dbeafe;font-size:17px}} .cards{{display:grid;grid-template-columns:repeat(5,1fr);gap:14px;margin:22px 0}}
.card,section{{background:white;border:1px solid var(--line);border-radius:18px;box-shadow:0 8px 25px #0f172a0b}} .card{{padding:20px}} .card span{{display:block;color:var(--muted);font-size:13px;margin-bottom:9px}} .card strong{{font-size:24px;color:var(--ink)}}
section{{padding:26px;margin:20px 0}} h2{{margin:0 0 18px;color:var(--ink)}} img{{display:block;width:100%;height:auto;border-radius:10px}}
table{{width:100%;border-collapse:collapse;font-variant-numeric:tabular-nums}} th,td{{padding:13px;border-bottom:1px solid var(--line);text-align:right}} th:first-child,td:first-child{{text-align:left}} th{{color:var(--muted);font-size:13px}} .pass{{color:var(--green);font-weight:800}} .fail{{color:var(--red);font-weight:800}}
footer{{color:var(--muted);font-size:13px;margin-top:22px}} @media(max-width:900px){{main{{padding:20px}}.cards{{grid-template-columns:repeat(2,1fr)}}}}
</style></head><body><main>
<header><h1>Cluverse Spike Traffic Report</h1><p>{html.escape(metadata['label'])} · {html.escape(metadata['profile'])} · {html.escape(metadata['run_id'])}</p></header>
<div class="cards">{card_html}</div>
<section><h2>SLO summary</h2><table><thead><tr><th>Phase</th><th>Target TPS</th><th>Completed RPS</th><th>Read p95 / p99</th><th>Write p95 / p99</th><th>Success</th><th>SLO</th></tr></thead><tbody>{rows_html}</tbody></table></section>
{images}
<footer>Generated from k6 CSV, Prometheus query_range and immutable run metadata. Raw evidence remains in this run directory.</footer>
</main></body></html>"""
    path = run_dir / "report.html"
    path.write_text(document, encoding="utf-8")
    return path


def build_summary(
    metadata: dict[str, Any], steps: list[dict[str, Any]], normalization: float | None
) -> dict[str, Any]:
    passing = [row for row in steps if row["slo_pass"]]
    return {
        "run_id": metadata["run_id"],
        "label": metadata["label"],
        "profile": metadata["profile"],
        "max_sustainable_tps": max((row["target_tps"] for row in passing), default=None),
        "normalization_seconds": normalization,
        "steps": steps,
        "slo": metadata["slo"],
    }


def json_safe(value: Any) -> Any:
    if isinstance(value, float) and not math.isfinite(value):
        return None
    if isinstance(value, dict):
        return {key: json_safe(item) for key, item in value.items()}
    if isinstance(value, list):
        return [json_safe(item) for item in value]
    return value


def main(argv: Sequence[str] | None = None) -> int:
    args = parse_args(argv)
    if args.bucket_seconds <= 0:
        raise ValueError("bucket-seconds는 1 이상이어야 합니다.")
    metadata = read_metadata(args.run_dir)
    raw_csv = args.run_dir / "k6-timeseries-raw.csv"
    if not raw_csv.exists():
        raise ValueError(f"k6 CSV가 없습니다: {raw_csv}")
    samples = metric_samples(raw_csv)
    timeseries = build_timeseries(samples, metadata, args.bucket_seconds)
    steps = build_step_summary(samples, metadata)
    normalization = normalization_seconds(timeseries, metadata, args.bucket_seconds)
    prometheus = load_prometheus(args.run_dir)

    write_csv(args.run_dir / "k6-timeseries.csv", K6_FIELDS, timeseries)
    write_csv(args.run_dir / "slo-steps.csv", STEP_FIELDS, steps)
    summary = build_summary(metadata, steps, normalization)
    (args.run_dir / "analysis-summary.json").write_text(
        json.dumps(json_safe(summary), ensure_ascii=False, indent=2, allow_nan=False), encoding="utf-8"
    )
    charts = [] if args.skip_charts else render_charts(args.run_dir, metadata, timeseries, steps, prometheus)
    report = render_html(args.run_dir, metadata, steps, normalization, charts)
    print(report)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
