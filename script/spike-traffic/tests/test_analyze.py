from __future__ import annotations

import math
import sys
import unittest
from pathlib import Path


SCRIPT_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPT_DIR))

from analyze import (
    build_step_summary,
    build_timeseries,
    json_safe,
    normalization_seconds,
    quantile,
)


def capacity_metadata() -> dict:
    return {
        "profile": "capacity",
        "start_epoch": 1000,
        "end_epoch": 1020,
        "load": {
            "capacity_rates": [10, 20],
            "step_duration_seconds": 10,
            "settle_seconds": 0,
            "smoke_rate": 1,
        },
        "slo": {
            "read_p95_ms": 300,
            "read_p99_ms": 800,
            "write_p95_ms": 500,
            "write_p99_ms": 1500,
            "success_rate": 0.99,
        },
    }


class AnalyzeTest(unittest.TestCase):

    def test_quantile은_선형_보간한다(self):
        self.assertAlmostEqual(3.7, quantile([1, 2, 3, 4], 0.9))

    def test_k6_sample을_5초_시계열로_정규화한다(self):
        metadata = capacity_metadata()
        samples = {
            "http_reqs": [(1000 + index / 10, 1) for index in range(50)],
            "core_read_duration": [(1000.1, 100), (1001.1, 200), (1002.1, 300)],
            "core_request_success": [(1000.1, 1), (1001.1, 1), (1002.1, 0)],
            "dropped_iterations": [(1003, 2)],
        }

        rows = build_timeseries(samples, metadata, 5)

        self.assertEqual(4, len(rows))
        self.assertEqual(10, rows[0]["completed_rps"])
        self.assertEqual(10, rows[0]["target_tps"])
        self.assertAlmostEqual(66.666666, rows[0]["success_rate_percent"], places=4)
        self.assertEqual(2, rows[0]["dropped_iterations"])

    def test_capacity_step은_SLO와_도착률을_함께_판정한다(self):
        metadata = capacity_metadata()
        samples = {
            "http_reqs": [
                *[(1000 + index / 10, 1) for index in range(100)],
                *[(1010 + index / 20, 1) for index in range(160)],
            ],
            "core_read_duration": [
                *[(1000 + index / 10, 100) for index in range(100)],
                *[(1010 + index / 20, 1000) for index in range(160)],
            ],
            "core_request_success": [
                *[(1000 + index / 10, 1) for index in range(100)],
                *[(1010 + index / 20, 1) for index in range(160)],
            ],
        }

        rows = build_step_summary(samples, metadata)

        self.assertTrue(rows[0]["slo_pass"])
        self.assertFalse(rows[1]["slo_pass"])

    def test_정상화는_연속된_안정_구간의_시작점을_반환한다(self):
        metadata = {
            "profile": "spike",
            "load": {
                "baseline_seconds": 10,
                "ramp_seconds": 5,
                "spike_seconds": 10,
                "normalization_window_seconds": 15,
            },
            "slo": {"read_p99_ms": 800, "write_p99_ms": 1500, "success_rate": 0.99},
        }
        rows = [
            {
                "elapsed_seconds": elapsed,
                "read_p99_ms": 1000 if elapsed == 26 else 200,
                "write_p99_ms": 300,
                "success_rate_percent": 100,
                "dropped_iterations": 0,
            }
            for elapsed in (26, 31, 36, 41, 46)
        ]

        self.assertEqual(5, normalization_seconds(rows, metadata, 5))

    def test_JSON에는_NaN을_null로_변환한다(self):
        self.assertEqual({"value": None}, json_safe({"value": math.nan}))


if __name__ == "__main__":
    unittest.main()
