from __future__ import annotations

import sys
import unittest
from pathlib import Path


SCRIPT_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPT_DIR))

from collect_prometheus import flatten_result


class CollectPrometheusTest(unittest.TestCase):

    def test_query_range_응답을_long_CSV_행으로_변환한다(self):
        spec = {"name": "hikari_pending", "title": "Hikari pending", "group": "app", "unit": "count"}
        payload = {
            "data": {
                "result": [
                    {"metric": {"pool": "HikariPool-1"}, "values": [[1000, "0"], [1015, "2"]]}
                ]
            }
        }

        rows = flatten_result(spec, payload, 1000)

        self.assertEqual(2, len(rows))
        self.assertEqual(15, rows[1]["elapsed_seconds"])
        self.assertEqual(2, rows[1]["value"])


if __name__ == "__main__":
    unittest.main()
