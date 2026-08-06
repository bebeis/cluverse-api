import csv
import json
import subprocess
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
COLLECTOR = ROOT / "script/popularity/collect_inline_results.py"


class CollectInlineResultsTest(unittest.TestCase):

    def test_k6_summary_is_appended_to_inline_metrics_csv(self):
        with tempfile.TemporaryDirectory() as directory:
            result_root = Path(directory) / "results"
            raw = result_root / "raw"
            raw.mkdir(parents=True)
            summary = raw / "20260806-120000-inline-disabled-like-r1-summary.json"
            summary.write_text(json.dumps({
                "metrics": {
                    "popularity_inline_api_duration": {
                        "values": {"avg": 10, "p(95)": 20, "p(99)": 30, "max": 40}
                    },
                    "popularity_inline_failures": {"values": {"rate": 0}},
                    "dropped_iterations": {"values": {"count": 0}},
                    "popularity_inline_completed": {"values": {"count": 300}},
                }
            }), encoding="utf-8")

            subprocess.run(["python3", str(COLLECTOR), str(summary)], check=True)

            with (result_root / "inline-metrics.csv").open(encoding="utf-8") as stream:
                row = next(csv.DictReader(stream))
            self.assertEqual("disabled", row["condition"])
            self.assertEqual("like", row["kind"])
            self.assertEqual("30.0", row["p99_ms"])
            self.assertEqual("300.0", row["completed"])

    def test_current_k6_flat_summary_is_supported(self):
        with tempfile.TemporaryDirectory() as directory:
            result_root = Path(directory) / "results"
            raw = result_root / "raw"
            raw.mkdir(parents=True)
            summary = raw / "20260806-130311-inline-disabled-like-r1-summary.json"
            summary.write_text(json.dumps({
                "metrics": {
                    "popularity_inline_api_duration": {
                        "avg": 40.2, "p(95)": 49.269, "p(99)": 124.227, "max": 516.152
                    },
                    "popularity_inline_failures": {"value": 0},
                    "dropped_iterations": {"count": 0},
                    "popularity_inline_completed": {"count": 301},
                }
            }), encoding="utf-8")

            subprocess.run(["python3", str(COLLECTOR), str(summary)], check=True)

            with (result_root / "inline-metrics.csv").open(encoding="utf-8") as stream:
                row = next(csv.DictReader(stream))
            self.assertEqual("49.269", row["p95_ms"])
            self.assertEqual("124.227", row["p99_ms"])
            self.assertEqual("301.0", row["completed"])


if __name__ == "__main__":
    unittest.main()
