#!/usr/bin/env python3
import argparse
import json
import os
import subprocess
from pathlib import Path


OBJECT_QUERY = """
SELECT u.status, u.staging_cleaned, a.staging_key, a.content_key, a.thumbnail_key
FROM post_image_upload u
LEFT JOIN post_image_asset a ON a.post_image_upload_id = u.post_image_upload_id
"""

STALE_QUERY = """
SELECT COUNT(*)
FROM post_image_upload
WHERE status = 'PENDING'
  AND updated_at < NOW(6) - INTERVAL 3 MINUTE
"""


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--bucket", required=True)
    parser.add_argument("--endpoint-url")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()

    rows = mysql_rows(OBJECT_QUERY)
    stale_pending = int(mysql_rows(STALE_QUERY)[0][0])
    existing_keys = list_s3_keys(args)
    expected_keys = set()
    completed_outputs = []
    completed_missing_keys = 0
    cleaned_staging = []

    for status, staging_cleaned, staging_key, content_key, thumbnail_key in rows:
        if status == "PENDING":
            expected_keys.update(key for key in (staging_key, content_key, thumbnail_key) if key)
        elif status == "COMPLETED":
            if not content_key or not thumbnail_key:
                completed_missing_keys += 1
            expected_keys.update(key for key in (content_key, thumbnail_key) if key)
            completed_outputs.extend(key for key in (content_key, thumbnail_key) if key)
            if staging_cleaned == "0" and staging_key:
                expected_keys.add(staging_key)
            elif staging_key:
                cleaned_staging.append(staging_key)

    evidence = {
        "completed_missing_objects": completed_missing_keys
        + sum(key not in existing_keys for key in completed_outputs),
        "unexpected_s3_objects": len(existing_keys - expected_keys),
        "completed_staging_left": sum(key in existing_keys for key in cleaned_staging),
        "stale_pending": stale_pending,
    }
    print("metric                         count")
    print("----------------------------- -----")
    for name, count in evidence.items():
        print(f"{name:<29} {count:>5}")

    if args.output:
        args.output.write_text(json.dumps(evidence, indent=2) + "\n", encoding="utf-8")

    if any(evidence.values()):
        raise SystemExit(1)


def mysql_rows(query):
    required = ("MYSQL_HOST", "MYSQL_USER", "MYSQL_PASSWORD", "MYSQL_DATABASE")
    missing = [name for name in required if not os.environ.get(name)]
    if missing:
        raise SystemExit("missing environment: " + ", ".join(missing))
    environment = os.environ.copy()
    environment["MYSQL_PWD"] = environment["MYSQL_PASSWORD"]
    command = [
        "mysql",
        "--batch",
        "--skip-column-names",
        "--host", environment["MYSQL_HOST"],
        "--port", environment.get("MYSQL_PORT", "3306"),
        "--user", environment["MYSQL_USER"],
        environment["MYSQL_DATABASE"],
        "--execute", " ".join(query.split()),
    ]
    result = subprocess.run(command, check=True, capture_output=True, text=True, env=environment)
    return [line.split("\t") for line in result.stdout.splitlines() if line]


def list_s3_keys(args):
    command = [
        "aws", "s3api", "list-objects-v2",
        "--bucket", args.bucket,
        "--prefix", "image-uploads/",
        "--output", "json",
    ]
    if args.endpoint_url:
        command.extend(["--endpoint-url", args.endpoint_url])
    result = subprocess.run(command, check=True, capture_output=True, text=True)
    payload = json.loads(result.stdout)
    return {item["Key"] for item in payload.get("Contents", [])}


if __name__ == "__main__":
    main()
