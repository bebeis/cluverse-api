#!/usr/bin/env python3
import argparse
import json
import urllib.parse
import urllib.request


QUERIES = {
    "platform_threads_max": "max(max_over_time(jvm_threads_live_threads[1m]))",
    "v2_executor_queue_max": "max(max_over_time(image_upload_platform_executor_queue[1m]))",
    "wait_p95_seconds": "histogram_quantile(0.95, sum by (le) (rate(image_upload_wait_duration_seconds_bucket[1m])))",
    "remote_p95_seconds": "histogram_quantile(0.95, sum by (le) (rate(image_upload_remote_duration_seconds_bucket[1m])))",
}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--prometheus", required=True)
    parser.add_argument("--start", required=True, type=float)
    parser.add_argument("--end", required=True, type=float)
    parser.add_argument("--step", default="5s")
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    result = {}
    for name, query in QUERIES.items():
        params = urllib.parse.urlencode({
            "query": query,
            "start": args.start,
            "end": args.end,
            "step": args.step,
        })
        url = f"{args.prometheus.rstrip('/')}/api/v1/query_range?{params}"
        with urllib.request.urlopen(url, timeout=10) as response:
            result[name] = json.load(response)["data"]["result"]

    with open(args.output, "w", encoding="utf-8") as output:
        json.dump(result, output, ensure_ascii=False, indent=2)


if __name__ == "__main__":
    main()
