from __future__ import annotations

import sys
import unittest
from pathlib import Path


SCRIPT_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPT_DIR))

from compare_runs import common_latency_tps, read_row


class CompareRunsTest(unittest.TestCase):

    def test_지연은_공통_SLO_처리량에서_비교한다(self):
        baseline = {
            "run_id": "baseline",
            "label": "baseline",
            "profile": "capacity",
            "max_sustainable_tps": 100,
            "normalization_seconds": None,
            "steps": [
                {"target_tps": 50, "slo_pass": True, "read_p99_ms": 100},
                {"target_tps": 100, "slo_pass": True, "read_p99_ms": 200},
                {"target_tps": 150, "slo_pass": False, "read_p99_ms": 900},
            ],
        }
        improved = {
            "run_id": "improved",
            "label": "improved",
            "profile": "capacity",
            "max_sustainable_tps": 150,
            "normalization_seconds": None,
            "steps": [
                {"target_tps": 50, "slo_pass": True, "read_p99_ms": 80},
                {"target_tps": 100, "slo_pass": True, "read_p99_ms": 120},
                {"target_tps": 150, "slo_pass": True, "read_p99_ms": 300},
            ],
        }

        comparison_tps = common_latency_tps([baseline, improved])
        baseline_row = read_row(baseline, comparison_tps)
        improved_row = read_row(improved, comparison_tps)

        self.assertEqual(100, comparison_tps)
        self.assertEqual(200, baseline_row["read_p99_ms"])
        self.assertEqual(120, improved_row["read_p99_ms"])


if __name__ == "__main__":
    unittest.main()
