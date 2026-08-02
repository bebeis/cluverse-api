from __future__ import annotations

import csv
import json
import tempfile
import unittest
from pathlib import Path

import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from plot_results import (
    collect,
    discover_inputs,
    extract_comment_count,
    extract_csv,
    extract_k6_summary,
    is_first_cursor_position,
    quantile,
)


class PlotResultsTest(unittest.TestCase):

    def test_k6_summary를_공통_지표로_변환한다(self):
        payload = {
            "options": {"tags": {"version": "v2"}},
            "metrics": {
                "local_map_write_duration": {
                    "values": {"avg": 10, "med": 9, "p(90)": 15, "p(95)": 18, "p(99)": 25, "max": 30}
                },
                "http_reqs": {"values": {"count": 100, "rate": 9.5}},
                "http_req_failed": {"values": {"rate": 0.01}},
                "local_map_write_success": {"values": {"rate": 0.99}},
            },
        }
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "local-map-2026-08-01-v2-summary.json"
            path.write_text(json.dumps(payload), encoding="utf-8")

            rows = extract_k6_summary(path, None, None)

        self.assertEqual(18, self.value(rows, "api_latency", "p95"))
        self.assertEqual(25, self.value(rows, "api_latency", "p99"))
        self.assertEqual(9.5, self.value(rows, "throughput", "rate"))
        self.assertEqual(1.0, self.value(rows, "failure_rate", "rate"))

    def test_정규화_CSV를_읽는다(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "local-map-metrics.csv"
            with path.open("w", encoding="utf-8", newline="") as file:
                writer = csv.DictWriter(file, fieldnames=(
                    "experiment", "version", "metric", "stat", "value", "unit"
                ))
                writer.writeheader()
                writer.writerow({
                    "experiment": "local-map", "version": "v1", "metric": "hikari_active",
                    "stat": "max", "value": "10", "unit": "count",
                })

            rows = extract_csv(path, None, None)

        self.assertEqual(10, self.value(rows, "hikari_active", "max"))

    def test_k6_원시_CSV에서_분위수를_계산한다(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "popularity-bench-v1.csv"
            with path.open("w", encoding="utf-8", newline="") as file:
                writer = csv.DictWriter(file, fieldnames=(
                    "metric_name", "timestamp", "metric_value", "version", "scenario"
                ))
                writer.writeheader()
                for timestamp, value in enumerate((10, 20, 30, 40), start=1):
                    writer.writerow({
                        "metric_name": "popularity_request_duration",
                        "timestamp": timestamp,
                        "metric_value": value,
                        "version": "v1",
                        "scenario": "bench",
                    })

            rows = extract_csv(path, None, None)

        self.assertAlmostEqual(38.5, self.value(rows, "api_latency", "p95"))

    def test_분위수는_선형_보간한다(self):
        self.assertAlmostEqual(3.7, quantile([1, 2, 3, 4], 0.9))

    def test_네_실험_fixture를_한꺼번에_수집한다(self):
        fixture_directory = Path(__file__).resolve().parent / "fixtures"

        rows = collect(discover_inputs([str(fixture_directory)]), None, None)

        self.assertEqual(
            {"popularity", "view-surge", "local-map", "comment-pagination"},
            {row.experiment for row in rows},
        )

    def test_댓글_수_시나리오를_그래프_x축으로_변환한다(self):
        self.assertEqual(1000, extract_comment_count("comments=1000,tree_shape=mixed"))

    def test_댓글_규모_그래프는_첫_커서_위치만_사용한다(self):
        self.assertTrue(is_first_cursor_position("comments=1000,cursor_position=first"))
        self.assertFalse(is_first_cursor_position("comments=1000,cursor_position=page-10"))
        self.assertFalse(is_first_cursor_position("comments=1000"))

    def value(self, rows, metric, stat):
        return next(row.value for row in rows if row.metric == metric and row.stat == stat)


if __name__ == "__main__":
    unittest.main()
