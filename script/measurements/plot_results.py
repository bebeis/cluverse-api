#!/usr/bin/env python3
"""Collect k6 summary JSON / CSV measurements and render matplotlib charts."""

from __future__ import annotations

import argparse
import csv
import json
import math
import os
import re
import statistics
import sys
import tempfile
from collections import defaultdict
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable, Sequence


EXPERIMENTS = ("popularity", "view-surge", "local-map", "comment-pagination")
TREND_METRICS = {
    "popularity": (
        "popularity_request_duration",
        "popularity_lifecycle_check_duration",
        "popularity_lifecycle_recent_duration",
        "popularity_first_visibility_delay",
        "http_req_duration",
    ),
    "view-surge": ("view_count_duration", "hot_duration", "http_req_duration"),
    "local-map": ("local_map_write_duration", "http_req_duration"),
    "comment-pagination": (
        "comment_api_duration",
        "detail_screen_duration",
        "comment_write_duration",
        "http_req_duration",
    ),
}
SUCCESS_METRICS = {
    "popularity": ("popularity_request_success_rate", "popularity_lifecycle_check_success_rate"),
    "view-surge": ("view_count_success_rate", "hot_success_rate"),
    "local-map": ("local_map_write_success",),
    "comment-pagination": ("comment_request_success", "comment_write_success", "comment_page_equivalence"),
}
CORE_METRICS = {
    "api_latency",
    "throughput",
    "failure_rate",
    "success_rate",
    "dropped_iterations",
}
SERIES_LABELS = {
    "popularity_request_duration": "request",
    "popularity_lifecycle_check_duration": "promotion check",
    "popularity_lifecycle_recent_duration": "recent list",
    "popularity_first_visibility_delay": "visibility delay",
    "view_count_duration": "view count",
    "hot_duration": "hotspot",
    "local_map_write_duration": "write",
    "comment_api_duration": "comment API",
    "detail_screen_duration": "detail screen",
    "comment_write_duration": "comment write",
    "http_req_duration": "HTTP",
}
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
class Measurement:
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


def parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="인기글·조회수 급상승·로컬맵·댓글 페이지 JSON/CSV를 모아 matplotlib 그래프를 생성합니다."
    )
    parser.add_argument("--input", action="append", required=True, help="입력 파일 또는 디렉터리(반복 가능)")
    parser.add_argument("--output-dir", default="script/measurements/results/latest")
    parser.add_argument("--experiment", choices=EXPERIMENTS, help="경로에서 실험 종류를 알 수 없을 때 사용")
    parser.add_argument("--version", help="파일명/JSON에서 버전을 알 수 없을 때 사용")
    parser.add_argument("--format", action="append", choices=("png", "svg"), dest="formats")
    parser.add_argument("--aggregate", choices=("none", "median", "latest"), default="none")
    parser.add_argument("--normalize-only", action="store_true", help="CSV만 만들고 그래프는 생략")
    return parser.parse_args(argv)


def discover_inputs(values: Sequence[str]) -> list[Path]:
    discovered: list[Path] = []
    for value in values:
        path = Path(value)
        if path.is_file():
            discovered.append(path)
            continue
        if not path.is_dir():
            raise ValueError(f"입력 경로가 없습니다: {path}")
        discovered.extend(
            candidate
            for candidate in path.rglob("*")
            if candidate.is_file()
            and (candidate.suffix.lower() == ".csv" or candidate.name.endswith("summary.json"))
        )
    return sorted(set(discovered))


def infer_experiment(path: Path, override: str | None) -> str:
    if override:
        return override
    normalized = str(path).lower().replace("_", "-")
    for experiment in EXPERIMENTS:
        if experiment in normalized:
            return experiment
    raise ValueError(f"실험 종류를 경로에서 판별할 수 없습니다: {path}. --experiment를 지정하세요.")


def infer_version(path: Path, payload: dict | None, override: str | None) -> str:
    if override:
        return override.lower()
    if payload:
        tags = payload.get("options", {}).get("tags", {})
        if isinstance(tags, dict) and tags.get("version"):
            return str(tags["version"]).lower()
    match = re.search(r"(?:^|[-_])(v\d+)(?:[-_.]|$)", path.name.lower())
    return match.group(1) if match else "unknown"


def infer_recorded_at(path: Path) -> str:
    match = re.search(r"(\d{4}-\d{2}-\d{2})-(\d{6})", path.name)
    if match:
        parsed = datetime.strptime("".join(match.groups()), "%Y-%m-%d%H%M%S")
        return parsed.astimezone().isoformat()
    return datetime.fromtimestamp(path.stat().st_mtime, tz=timezone.utc).isoformat()


def parse_tagged_metric(name: str) -> tuple[str, str]:
    match = re.fullmatch(r"([^{}]+)(?:\{([^}]*)\})?", name)
    if not match:
        return name, ""
    raw_tags = match.group(2) or ""
    tags = []
    for token in raw_tags.split(","):
        token = token.strip()
        if not token:
            continue
        tags.append(token.replace(":", "=", 1))
    return match.group(1), ",".join(sorted(tags))


def option_scenario(payload: dict) -> str:
    tags = payload.get("options", {}).get("tags", {})
    if not isinstance(tags, dict):
        return ""
    return ",".join(f"{key}={tags[key]}" for key in sorted(tags) if key != "version")


def combine_scenario(*parts: str) -> str:
    return ",".join(part for part in parts if part)


def infer_run_kind(path: Path) -> str:
    lowered = path.stem.lower()
    for kind in ("bench", "lifecycle", "recall", "correctness"):
        if kind in lowered:
            return f"kind={kind}"
    return ""


def measurement(
    *,
    path: Path,
    experiment: str,
    version: str,
    scenario: str,
    source: str,
    metric: str,
    stat: str,
    value: float,
    unit: str,
    recorded_at: str | None = None,
) -> Measurement:
    return Measurement(
        experiment=experiment,
        run_id=path.stem.removesuffix("-summary"),
        recorded_at=recorded_at or infer_recorded_at(path),
        version=version,
        scenario=scenario or "default",
        source=source,
        metric=metric,
        stat=stat,
        value=float(value),
        unit=unit,
        input_file=str(path),
    )


def extract_k6_summary(path: Path, experiment_override: str | None, version_override: str | None) -> list[Measurement]:
    with path.open(encoding="utf-8") as file:
        payload = json.load(file)
    metrics = payload.get("metrics")
    if not isinstance(metrics, dict):
        raise ValueError(f"k6 summary JSON의 metrics가 없습니다: {path}")
    experiment = infer_experiment(path, experiment_override)
    version = infer_version(path, payload, version_override)
    base_scenario = combine_scenario(option_scenario(payload), infer_run_kind(path))
    rows: list[Measurement] = []

    preferred = TREND_METRICS[experiment]
    available_bases = {parse_tagged_metric(name)[0] for name in metrics}
    chosen_trends = [name for name in preferred if name != "http_req_duration" and name in available_bases]
    if not chosen_trends and "http_req_duration" in available_bases:
        chosen_trends = ["http_req_duration"]
    if chosen_trends:
        for name, metric_data in metrics.items():
            base_name, metric_scenario = parse_tagged_metric(name)
            if base_name not in chosen_trends:
                continue
            values = metric_data.get("values", {})
            for stat in ("avg", "med", "p(90)", "p(95)", "p(99)", "max"):
                if stat in values:
                    rows.append(measurement(
                        path=path,
                        experiment=experiment,
                        version=version,
                        scenario=combine_scenario(base_scenario, f"series={base_name}", metric_scenario),
                        source="k6-summary",
                        metric="api_latency",
                        stat=normalize_stat(stat),
                        value=values[stat],
                        unit="ms",
                    ))

    append_k6_scalar(rows, path, experiment, version, base_scenario, metrics, "http_reqs", "rate",
                     "throughput", "rate", "req/s")
    append_k6_scalar(rows, path, experiment, version, base_scenario, metrics, "http_req_failed", "rate",
                     "failure_rate", "rate", "%", multiplier=100)
    append_k6_scalar(rows, path, experiment, version, base_scenario, metrics, "dropped_iterations", "count",
                     "dropped_iterations", "count", "count")

    for success_metric in SUCCESS_METRICS[experiment]:
        if success_metric in metrics:
            append_k6_scalar(rows, path, experiment, version, base_scenario, metrics, success_metric, "rate",
                             "success_rate", "rate", "%", multiplier=100)
            break
    return rows


def append_k6_scalar(
    rows: list[Measurement],
    path: Path,
    experiment: str,
    version: str,
    scenario: str,
    metrics: dict,
    source_metric: str,
    source_stat: str,
    target_metric: str,
    target_stat: str,
    unit: str,
    multiplier: float = 1,
) -> None:
    metric_data = metrics.get(source_metric)
    if not isinstance(metric_data, dict):
        return
    value = metric_data.get("values", {}).get(source_stat)
    if value is None:
        return
    rows.append(measurement(
        path=path,
        experiment=experiment,
        version=version,
        scenario=scenario,
        source="k6-summary",
        metric=target_metric,
        stat=target_stat,
        value=float(value) * multiplier,
        unit=unit,
    ))


def normalize_stat(value: str) -> str:
    return {
        "p(90)": "p90",
        "p(95)": "p95",
        "p(99)": "p99",
    }.get(value, value)


def extract_csv(path: Path, experiment_override: str | None, version_override: str | None) -> list[Measurement]:
    with path.open(encoding="utf-8-sig", newline="") as file:
        reader = csv.DictReader(file)
        fieldnames = set(reader.fieldnames or ())
        records = list(reader)
    if {"metric", "stat", "value"}.issubset(fieldnames):
        return extract_normalized_csv(path, records, experiment_override, version_override)
    if {"metric_name", "metric_value"}.issubset(fieldnames):
        return extract_k6_csv(path, records, experiment_override, version_override)
    raise ValueError(
        f"지원하지 않는 CSV 스키마입니다: {path}. metric/stat/value 또는 metric_name/metric_value가 필요합니다."
    )


def extract_normalized_csv(
    path: Path,
    records: list[dict[str, str]],
    experiment_override: str | None,
    version_override: str | None,
) -> list[Measurement]:
    rows: list[Measurement] = []
    for record in records:
        experiment = record.get("experiment") or infer_experiment(path, experiment_override)
        version = (record.get("version") or version_override or infer_version(path, None, None)).lower()
        raw_value = (record.get("value") or "").strip().replace(",", "")
        multiplier = 1.0
        if raw_value.endswith("%"):
            raw_value = raw_value[:-1]
        try:
            numeric = float(raw_value) * multiplier
        except ValueError as error:
            raise ValueError(f"CSV value가 숫자가 아닙니다: {path}: {record.get('value')}") from error
        rows.append(Measurement(
            experiment=experiment,
            run_id=record.get("run_id") or path.stem,
            recorded_at=record.get("recorded_at") or infer_recorded_at(path),
            version=version,
            scenario=record.get("scenario") or "default",
            source=record.get("source") or "csv",
            metric=record["metric"],
            stat=normalize_stat(record["stat"]),
            value=numeric,
            unit=record.get("unit") or "",
            input_file=record.get("input_file") or str(path),
        ))
    return rows


def extract_k6_csv(
    path: Path,
    records: list[dict[str, str]],
    experiment_override: str | None,
    version_override: str | None,
) -> list[Measurement]:
    experiment = infer_experiment(path, experiment_override)
    default_version = version_override or infer_version(path, None, None)
    grouped: dict[tuple[str, str, str], list[tuple[float, float]]] = defaultdict(list)
    for record in records:
        name = record.get("metric_name", "")
        try:
            value = float(record.get("metric_value", ""))
            timestamp = float(record.get("timestamp", "0") or 0)
        except ValueError:
            continue
        version = (record.get("version") or default_version).lower()
        scenario = combine_scenario(
            record.get("scenario") or record.get("step") or "",
            infer_run_kind(path),
        ) or "default"
        grouped[(name, version, scenario)].append((timestamp, value))

    rows: list[Measurement] = []
    preferred = TREND_METRICS[experiment]
    available = {name for name, _, _ in grouped}
    chosen_trends = {name for name in preferred if name != "http_req_duration" and name in available}
    if not chosen_trends and "http_req_duration" in available:
        chosen_trends = {"http_req_duration"}
    for (name, version, scenario), samples in grouped.items():
        values = [value for _, value in samples]
        if name in chosen_trends:
            scenario = combine_scenario(scenario, f"series={name}")
            for stat, quantile_value in (
                ("avg", statistics.fmean(values)),
                ("med", quantile(values, 0.5)),
                ("p90", quantile(values, 0.9)),
                ("p95", quantile(values, 0.95)),
                ("p99", quantile(values, 0.99)),
                ("max", max(values)),
            ):
                rows.append(measurement(
                    path=path, experiment=experiment, version=version, scenario=scenario,
                    source="k6-csv", metric="api_latency", stat=stat, value=quantile_value, unit="ms",
                ))
        elif name == "http_req_failed":
            rows.append(measurement(
                path=path, experiment=experiment, version=version, scenario=scenario,
                source="k6-csv", metric="failure_rate", stat="rate",
                value=statistics.fmean(values) * 100, unit="%",
            ))
        elif name in SUCCESS_METRICS[experiment]:
            rows.append(measurement(
                path=path, experiment=experiment, version=version, scenario=scenario,
                source="k6-csv", metric="success_rate", stat="rate",
                value=statistics.fmean(values) * 100, unit="%",
            ))
        elif name == "http_reqs":
            timestamps = [timestamp for timestamp, _ in samples if timestamp > 0]
            elapsed = max(timestamps) - min(timestamps) if len(timestamps) > 1 else 0
            rate = sum(values) / elapsed if elapsed > 0 else 0
            rows.append(measurement(
                path=path, experiment=experiment, version=version, scenario=scenario,
                source="k6-csv", metric="throughput", stat="rate", value=rate, unit="req/s",
            ))
    return rows


def quantile(values: Sequence[float], probability: float) -> float:
    if not values:
        raise ValueError("빈 표본의 분위수를 계산할 수 없습니다.")
    ordered = sorted(values)
    position = (len(ordered) - 1) * probability
    lower = math.floor(position)
    upper = math.ceil(position)
    if lower == upper:
        return ordered[lower]
    return ordered[lower] + (ordered[upper] - ordered[lower]) * (position - lower)


def collect(paths: Iterable[Path], experiment: str | None, version: str | None) -> list[Measurement]:
    rows: list[Measurement] = []
    failures: list[str] = []
    for path in paths:
        try:
            if path.suffix.lower() == ".json":
                rows.extend(extract_k6_summary(path, experiment, version))
            elif path.suffix.lower() == ".csv":
                rows.extend(extract_csv(path, experiment, version))
        except (OSError, ValueError, json.JSONDecodeError) as error:
            failures.append(str(error))
    if failures:
        raise ValueError("\n".join(failures))
    if not rows:
        raise ValueError("수집된 측정값이 없습니다.")
    return sorted(rows, key=lambda row: (row.experiment, row.recorded_at, row.version, row.metric, row.stat))


def write_csv(rows: Sequence[Measurement], path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=CSV_FIELDS)
        writer.writeheader()
        for row in rows:
            writer.writerow(asdict(row))


def aggregate_rows(rows: Sequence[Measurement], mode: str) -> list[Measurement]:
    if mode == "none":
        return list(rows)
    grouped: dict[tuple[str, str, str, str, str, str], list[Measurement]] = defaultdict(list)
    for row in rows:
        grouped[(row.experiment, row.version, row.scenario, row.metric, row.stat, row.unit)].append(row)
    aggregated: list[Measurement] = []
    for values in grouped.values():
        if mode == "latest":
            aggregated.append(max(values, key=lambda value: value.recorded_at))
            continue
        template = max(values, key=lambda value: value.recorded_at)
        aggregated.append(Measurement(
            experiment=template.experiment,
            run_id=f"median-n{len(values)}",
            recorded_at=template.recorded_at,
            version=template.version,
            scenario=template.scenario,
            source="aggregate",
            metric=template.metric,
            stat=template.stat,
            value=statistics.median(value.value for value in values),
            unit=template.unit,
            input_file=";".join(sorted({value.input_file for value in values})),
        ))
    return aggregated


def render(rows: Sequence[Measurement], output_dir: Path, formats: Sequence[str]) -> list[Path]:
    os.environ.setdefault("MPLCONFIGDIR", str(Path(tempfile.gettempdir()) / "cluverse-matplotlib-cache"))
    try:
        import matplotlib
        matplotlib.use("Agg")
        import matplotlib.pyplot as plt
    except ModuleNotFoundError as error:
        raise RuntimeError(
            "matplotlib이 없습니다. python3 -m pip install -r script/measurements/requirements.txt 를 실행하세요."
        ) from error

    generated: list[Path] = []
    for experiment in sorted({row.experiment for row in rows}):
        experiment_rows = [row for row in rows if row.experiment == experiment]
        latency = [row for row in experiment_rows if row.metric == "api_latency" and row.stat in {"p95", "p99"}]
        if latency:
            generated.extend(plot_grouped_bars(
                plt, latency, output_dir, formats, f"{experiment}-latency", "API latency", "ms", ("p95", "p99")
            ))
        traffic = [row for row in experiment_rows if row.metric in {"throughput", "failure_rate"}]
        if traffic:
            generated.extend(plot_traffic(plt, traffic, output_dir, formats, experiment))
        resources = [row for row in experiment_rows if row.metric not in CORE_METRICS]
        if resources:
            generated.extend(plot_resources(plt, resources, output_dir, formats, experiment))
        steps = [row for row in latency if extract_step(row.scenario) is not None]
        if steps:
            generated.extend(plot_steps(plt, steps, output_dir, formats, experiment))
        if experiment == "comment-pagination":
            generated.extend(plot_comment_scale(plt, experiment_rows, output_dir, formats))
    return generated


def row_label(row: Measurement, include_run: bool = False) -> str:
    scenario_parts = []
    for token in row.scenario.split(","):
        if not token or token in {"default", "kind=bench"}:
            continue
        if token.startswith("series="):
            raw_series = token.split("=", 1)[1]
            scenario_parts.append(SERIES_LABELS.get(raw_series, raw_series))
        else:
            scenario_parts.append(token)
    parts = [row.version]
    if scenario_parts:
        parts.append(" / ".join(scenario_parts))
    if include_run:
        timestamp = re.search(r"(\d{4}-\d{2}-\d{2})-(\d{6})", row.run_id)
        if timestamp:
            clock = timestamp.group(2)
            parts.append(f"{timestamp.group(1)[5:]} {clock[:2]}:{clock[2:4]}:{clock[4:]}")
        else:
            parts.append(row.run_id[-20:])
    return "\n".join(parts)


def group_by_run(rows: Sequence[Measurement]) -> dict[tuple[str, str, str], dict[str, Measurement]]:
    grouped: dict[tuple[str, str, str], dict[str, Measurement]] = defaultdict(dict)
    for row in rows:
        grouped[(row.version, row.scenario, row.run_id)][row.stat] = row
    return dict(grouped)


def plot_grouped_bars(plt, rows, output_dir, formats, name, title, ylabel, stats) -> list[Path]:
    grouped = group_by_run(rows)
    keys = sorted(grouped, key=lambda key: (version_number(key[0]), key[1], key[2]))
    width = 0.36
    figure, axis = plt.subplots(figsize=(max(8, len(keys) * 1.5), 5.5))
    positions = list(range(len(keys)))
    run_counts: dict[tuple[str, str], int] = defaultdict(int)
    for version, scenario, _ in keys:
        run_counts[(version, scenario)] += 1
    for index, stat in enumerate(stats):
        values = [grouped[key].get(stat).value if grouped[key].get(stat) else math.nan for key in keys]
        bars = axis.bar([position + (index - 0.5) * width for position in positions], values, width, label=stat)
        axis.bar_label(bars, fmt="%.1f", padding=3, fontsize=8)
    axis.set_title(title)
    axis.set_ylabel(ylabel)
    axis.set_xticks(positions, [
        row_label(next(iter(grouped[key].values())), run_counts[(key[0], key[1])] > 1) for key in keys
    ], rotation=15, ha="right")
    axis.grid(axis="y", alpha=0.25)
    axis.legend()
    figure.tight_layout()
    return save_figure(figure, plt, output_dir, formats, name)


def plot_traffic(plt, rows, output_dir, formats, experiment) -> list[Path]:
    figure, axes = plt.subplots(1, 2, figsize=(12, 5))
    for axis, metric, ylabel in (
        (axes[0], "throughput", "req/s"),
        (axes[1], "failure_rate", "%"),
    ):
        values = [row for row in rows if row.metric == metric]
        values.sort(key=lambda row: (version_number(row.version), row.scenario, row.run_id))
        run_counts: dict[tuple[str, str], int] = defaultdict(int)
        for row in values:
            run_counts[(row.version, row.scenario)] += 1
        bars = axis.bar(range(len(values)), [row.value for row in values], color="#4C78A8" if metric == "throughput" else "#E45756")
        axis.bar_label(bars, fmt="%.2f", padding=3, fontsize=8)
        axis.set_title(metric.replace("_", " ").title())
        axis.set_ylabel(ylabel)
        axis.set_xticks(range(len(values)), [
            row_label(row, run_counts[(row.version, row.scenario)] > 1) for row in values
        ], rotation=15, ha="right")
        axis.grid(axis="y", alpha=0.25)
    figure.suptitle(f"{experiment} traffic")
    figure.tight_layout()
    return save_figure(figure, plt, output_dir, formats, f"{experiment}-traffic")


def plot_resources(plt, rows, output_dir, formats, experiment) -> list[Path]:
    latest: dict[tuple[str, str, str, str], Measurement] = {}
    for row in rows:
        key = (row.version, row.metric, row.stat, row.unit)
        if key not in latest or row.recorded_at > latest[key].recorded_at:
            latest[key] = row
    values = sorted(latest.values(), key=lambda row: (row.metric, row.stat, version_number(row.version)))
    metrics = sorted({row.metric for row in values})
    figure, axes = plt.subplots(
        1, len(metrics), figsize=(max(7, len(metrics) * 4.5), 5.5), squeeze=False
    )
    for axis, metric in zip(axes[0], metrics):
        metric_values = [row for row in values if row.metric == metric]
        labels = [f"{row.version}\n{row.stat}" for row in metric_values]
        bars = axis.bar(range(len(metric_values)), [row.value for row in metric_values], color="#72B7B2")
        axis.bar_label(bars, fmt="%.2f", padding=3, fontsize=8)
        axis.set_title(metric.replace("_", " "))
        axis.set_ylabel(metric_values[0].unit)
        axis.set_xticks(range(len(metric_values)), labels)
        axis.grid(axis="y", alpha=0.25)
    figure.suptitle(f"{experiment} additional metrics (latest)")
    figure.tight_layout()
    return save_figure(figure, plt, output_dir, formats, f"{experiment}-additional-metrics")


def plot_steps(plt, rows, output_dir, formats, experiment) -> list[Path]:
    figure, axis = plt.subplots(figsize=(8, 5.5))
    for version in sorted({row.version for row in rows}, key=version_number):
        for stat in ("p95", "p99"):
            points = sorted(
                ((extract_step(row.scenario), row.value) for row in rows if row.version == version and row.stat == stat),
                key=lambda point: point[0],
            )
            if points:
                axis.plot([point[0] for point in points], [point[1] for point in points], marker="o", label=f"{version} {stat}")
    axis.set_title(f"{experiment} step latency")
    axis.set_xlabel("arrival rate (req/s)")
    axis.set_ylabel("ms")
    axis.grid(alpha=0.25)
    axis.legend()
    figure.tight_layout()
    return save_figure(figure, plt, output_dir, formats, f"{experiment}-steps")


def extract_step(scenario: str) -> int | None:
    match = re.search(r"(?:step[=:])r?(\d+)", scenario)
    return int(match.group(1)) if match else None


def extract_comment_count(scenario: str) -> int | None:
    match = re.search(r"(?:^|,)comments=(\d+)(?:,|$)", scenario)
    return int(match.group(1)) if match else None


def display_comment_version(version: str) -> str:
    return {"v1": "Before", "v2": "After"}.get(version.lower(), version)


def plot_comment_scale(plt, rows, output_dir, formats) -> list[Path]:
    generated: list[Path] = []
    latency_rows = [
        row for row in rows
        if row.metric == "api_latency"
        and row.stat in {"p95", "p99"}
        and extract_comment_count(row.scenario) is not None
        and any(f"series={series}" in row.scenario for series in ("comment_api_duration", "detail_screen_duration"))
    ]
    if latency_rows:
        figure, axes = plt.subplots(1, 2, figsize=(13, 5.5), squeeze=False)
        for axis, series, title in (
            (axes[0][0], "comment_api_duration", "Comment API latency"),
            (axes[0][1], "detail_screen_duration", "Detail screen completion"),
        ):
            series_rows = [row for row in latency_rows if f"series={series}" in row.scenario]
            for version in sorted({row.version for row in series_rows}, key=version_number):
                for stat in ("p95", "p99"):
                    points = sorted(
                        (
                            (extract_comment_count(row.scenario), row.value)
                            for row in series_rows
                            if row.version == version and row.stat == stat
                        ),
                        key=lambda point: point[0],
                    )
                    if points:
                        axis.plot(
                            [point[0] for point in points],
                            [point[1] for point in points],
                            marker="o",
                            label=f"{display_comment_version(version)} {stat}",
                        )
            axis.set_title(title)
            axis.set_xlabel("total comments")
            axis.set_ylabel("ms")
            axis.grid(alpha=0.25)
            axis.legend()
        figure.tight_layout()
        generated.extend(save_figure(
            figure, plt, output_dir, formats, "comment-pagination-scale-latency"
        ))

    actual_rows = [
        row for row in rows
        if row.metric == "actual_rows"
        and extract_comment_count(row.scenario) is not None
    ]
    if actual_rows:
        figure, axis = plt.subplots(figsize=(8, 5.5))
        for version in sorted({row.version for row in actual_rows}, key=version_number):
            points = sorted(
                (
                    (extract_comment_count(row.scenario), row.value)
                    for row in actual_rows
                    if row.version == version
                ),
                key=lambda point: point[0],
            )
            axis.plot(
                [point[0] for point in points],
                [point[1] for point in points],
                marker="o",
                label=display_comment_version(version),
            )
        axis.set_title("Rows visited by page selection")
        axis.set_xlabel("total comments")
        axis.set_ylabel("actual rows")
        axis.grid(alpha=0.25)
        axis.legend()
        figure.tight_layout()
        generated.extend(save_figure(
            figure, plt, output_dir, formats, "comment-pagination-scale-rows"
        ))
    return generated


def version_number(version: str) -> tuple[int, str]:
    match = re.fullmatch(r"v(\d+)", version.lower())
    return (int(match.group(1)), version) if match else (10_000, version)


def save_figure(figure, plt, output_dir: Path, formats: Sequence[str], name: str) -> list[Path]:
    generated = []
    for image_format in formats:
        path = output_dir / f"{name}.{image_format}"
        figure.savefig(path, dpi=160, bbox_inches="tight")
        generated.append(path)
    plt.close(figure)
    return generated


def main(argv: Sequence[str] | None = None) -> int:
    args = parse_args(argv)
    try:
        inputs = discover_inputs(args.input)
        if not inputs:
            raise ValueError("summary JSON 또는 CSV 입력 파일을 찾지 못했습니다.")
        rows = collect(inputs, args.experiment, args.version)
        output_dir = Path(args.output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)
        normalized_path = output_dir / "measurements.csv"
        write_csv(rows, normalized_path)
        generated = [normalized_path]
        if not args.normalize_only:
            plot_rows = aggregate_rows(rows, args.aggregate)
            generated.extend(render(plot_rows, output_dir, args.formats or ["png"]))
        print(f"입력 파일: {len(inputs)}개, 정규화 측정값: {len(rows)}개")
        for path in generated:
            print(path)
        return 0
    except (OSError, ValueError, RuntimeError) as error:
        print(f"오류: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
