#!/usr/bin/env python3
import argparse
import html
import json
from pathlib import Path


def metric(summary, name, key, default=0):
    values = summary.get("metrics", {}).get(name, {}).get("values", {})
    return float(values.get(key, default))


def load_rows(result_dir):
    rows = []
    for version in ("v1", "v2", "v3"):
        path = result_dir / f"{version}-summary.json"
        if not path.exists():
            continue
        with path.open(encoding="utf-8") as source:
            summary = json.load(source)
        prometheus = load_prometheus(result_dir / f"{version}-prometheus.json")
        rows.append({
            "version": version,
            "p95": metric(summary, "image_upload_duration", "p(95)"),
            "p99": metric(summary, "image_upload_duration", "p(99)"),
            "rate": metric(summary, "image_upload_requests", "rate"),
            "failure": metric(summary, "image_upload_failures", "rate") * 100,
            "control_p95": metric(summary, "http_req_duration{endpoint:control}", "p(95)"),
            "output_mb": metric(summary, "image_upload_output_bytes", "avg") / 1024 / 1024,
            "reduction": metric(summary, "image_upload_reduction_percent", "avg"),
            "threads": prometheus.get("platform_threads_max", 0),
            "queue": prometheus.get("v2_executor_queue_max", 0),
            "wait_p95": prometheus.get("wait_p95_seconds", 0) * 1000,
            "remote_p95": prometheus.get("remote_p95_seconds", 0) * 1000,
        })
    return rows


def load_prometheus(path):
    if not path.exists():
        return {}
    with path.open(encoding="utf-8") as source:
        raw = json.load(source)
    result = {}
    for name, series_list in raw.items():
        values = []
        for series in series_list:
            values.extend(float(item[1]) for item in series.get("values", []))
        result[name] = max(values, default=0)
    return result


def markdown(rows, metadata):
    lines = [
        "# devlog-11 image upload evidence",
        "",
        "동일한 fixture, 외부 processor, S3 bucket, 외부 동시 실행 한도에서 측정한 값이다.",
        "",
        f"- 대상: `{metadata['base_url']}`",
        f"- fixture: `{metadata['image_name']}` ({metadata['image_bytes']:,} bytes)",
        f"- 요청당 이미지: {metadata['image_count']}개",
        f"- upload VU / 실행 시간: {metadata['vus']} / {metadata['duration']}",
        f"- control API: {metadata['control_rate']} req/s",
        "",
        "| 버전 | 실행 모델 | 업로드 p95 (ms) | p99 (ms) | 처리량 (req/s) | 실패율 | control API p95 (ms) |",
        "|---|---|---:|---:|---:|---:|---:|",
    ]
    models = {
        "v1": "순차 동기 호출",
        "v2": "CompletableFuture + Platform",
        "v3": "CompletableFuture + Virtual + Semaphore",
    }
    for row in rows:
        lines.append(
            f"| {row['version']} | {models[row['version']]} | {row['p95']:.1f} | {row['p99']:.1f} | "
            f"{row['rate']:.2f} | {row['failure']:.2f}% | {row['control_p95']:.1f} |"
        )
    if any(row["threads"] or row["queue"] or row["wait_p95"] for row in rows):
        lines.extend([
            "",
            "| 버전 | platform threads max | 대기 p95 (ms) | 원격 호출 p95 (ms) | V2 queue max |",
            "|---|---:|---:|---:|---:|",
        ])
        for row in rows:
            lines.append(
                f"| {row['version']} | {row['threads']:.0f} | {row['wait_p95']:.1f} | "
                f"{row['remote_p95']:.1f} | {row['queue']:.0f} |"
            )
    lines.extend([
        "",
        "이미지 압축률은 실행 모델의 우열 지표가 아니다. 세 버전이 같은 외부 processor 정책을 호출했는지 확인하는 통제 지표다.",
        "",
        "| 버전 | 평균 결과 크기 (MiB) | 평균 감소율 |",
        "|---|---:|---:|",
    ])
    for row in rows:
        lines.append(f"| {row['version']} | {row['output_mb']:.2f} | {row['reduction']:.2f}% |")
    return "\n".join(lines) + "\n"


def svg(rows, metadata):
    width, height = 1000, 520
    max_latency = max((row["p99"] for row in rows), default=1) or 1
    colors = {"v1": "#ef4444", "v2": "#3b82f6", "v3": "#10b981"}
    parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">',
        '<rect width="100%" height="100%" fill="#0f172a"/>',
        '<text x="50" y="48" fill="#f8fafc" font-family="sans-serif" font-size="24" font-weight="700">devlog-11 external image processor latency</text>',
        '<text x="50" y="76" fill="#94a3b8" font-family="sans-serif" font-size="14">'
        f"이미지 {metadata['image_count']}개/요청 · VU {metadata['vus']} · {html.escape(metadata['duration'])} · 낮을수록 좋음</text>",
    ]
    for index, row in enumerate(rows):
        y = 125 + index * 120
        p95_width = 700 * row["p95"] / max_latency
        p99_width = 700 * row["p99"] / max_latency
        label = html.escape(row["version"].upper())
        parts.extend([
            f'<text x="50" y="{y + 24}" fill="#f8fafc" font-family="sans-serif" font-size="18" font-weight="700">{label}</text>',
            f'<rect x="120" y="{y}" width="{p99_width:.1f}" height="32" rx="4" fill="{colors[row["version"]]}" opacity="0.35"/>',
            f'<rect x="120" y="{y + 40}" width="{p95_width:.1f}" height="32" rx="4" fill="{colors[row["version"]]}"/>',
            f'<text x="{130 + p99_width:.1f}" y="{y + 22}" fill="#cbd5e1" font-family="monospace" font-size="14">p99 {row["p99"]:.0f} ms</text>',
            f'<text x="{130 + p95_width:.1f}" y="{y + 62}" fill="#f8fafc" font-family="monospace" font-size="14">p95 {row["p95"]:.0f} ms</text>',
        ])
    parts.append('</svg>')
    return "\n".join(parts)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--results", required=True, type=Path)
    parser.add_argument("--base-url", default="unknown")
    parser.add_argument("--image-file", type=Path)
    parser.add_argument("--image-count", default="unknown")
    parser.add_argument("--vus", default="unknown")
    parser.add_argument("--duration", default="unknown")
    parser.add_argument("--control-rate", default="unknown")
    args = parser.parse_args()
    rows = load_rows(args.results)
    if not rows:
        raise SystemExit("summary JSON이 없습니다")
    metadata = {
        "base_url": args.base_url,
        "image_name": args.image_file.name if args.image_file else "unknown",
        "image_bytes": args.image_file.stat().st_size if args.image_file else 0,
        "image_count": args.image_count,
        "vus": args.vus,
        "duration": args.duration,
        "control_rate": args.control_rate,
    }
    (args.results / "evidence.md").write_text(markdown(rows, metadata), encoding="utf-8")
    (args.results / "latency.svg").write_text(svg(rows, metadata), encoding="utf-8")
    print(markdown(rows, metadata))


if __name__ == "__main__":
    main()
