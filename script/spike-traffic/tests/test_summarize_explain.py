from __future__ import annotations

import sys
import unittest
from pathlib import Path


EXPLAIN_DIR = Path(__file__).resolve().parents[1] / "explain"
sys.path.insert(0, str(EXPLAIN_DIR))

from summarize_explain import parse_nodes, summarize


class SummarizeExplainTest(unittest.TestCase):

    def test_actual_rows와_loops를_노드별로_보존한다(self):
        text = """
-> Limit: 10 row(s)  (cost=10 rows=10) (actual time=0.100..12.000 rows=10 loops=1)
    -> Index lookup on p using idx_post_status  (cost=5 rows=100) (actual time=0.050..8.000 rows=100 loops=10)
"""

        nodes = parse_nodes(text)

        self.assertEqual(2, len(nodes))
        self.assertEqual(1000, nodes[1]["output_rows"])
        self.assertEqual(12, nodes[0]["actual_end_ms"])

    def test_실행계획의_sort와_table_scan을_표시한다(self):
        text = """
-> Sort: p.created_at  (actual time=1.000..2.000 rows=10 loops=1)
    -> Table scan on p  (actual time=0.100..1.000 rows=1000 loops=1)
"""
        nodes = parse_nodes(text)

        summary = summarize(text, nodes, "post list")

        self.assertTrue(summary["uses_sort"])
        self.assertTrue(summary["uses_table_scan"])
        self.assertEqual(1000, summary["largest_output_rows"])


if __name__ == "__main__":
    unittest.main()
