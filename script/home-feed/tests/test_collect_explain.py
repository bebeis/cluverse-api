from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from collect_explain import extract_node, measurements


class CollectExplainTest(unittest.TestCase):

    def test_개선_전_댓글_스캔_행을_읽는다(self):
        text = """
        -> Aggregate using temporary table
            -> Index scan on c using idx_comment_post_created_latest
               (actual time=0.100..22.000 rows=100000 loops=1)
        """

        self.assertEqual((100000.0, 1.0), extract_node(text, "v1"))

    def test_중간_개선의_Loose_Index_Scan_그룹_행을_읽는다(self):
        text = """
        -> Covering index skip scan for grouping on c using idx_comment_post_visible_created
           (actual time=0.010..0.030 rows=1000 loops=1)
        """

        self.assertEqual((1000.0, 1.0), extract_node(text, "v2"))

    def test_개선_후_활동_인덱스_행을_읽는다(self):
        text = """
        -> Covering index scan on activity using idx_post_comment_activity_latest
           (actual time=0.010..0.030 rows=10 loops=1)
        """

        self.assertEqual((10.0, 1.0), extract_node(text, "v3"))

    def test_Loose_Index_Scan_사용_여부를_기록한다(self):
        text = """
        Extra: Using index for group-by; Using temporary; Using filesort
        -> Covering index skip scan for grouping on c using idx_comment_post_visible_created
           (actual time=0.010..0.030 rows=1000 loops=1)
        """
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "v2.txt"
            path.write_text(text, encoding="utf-8")

            rows = measurements(path, "v2", "comments=100000", "2026-08-03T00:00:00+09:00")

        uses_loose_index = next(row for row in rows if row.metric == "uses_loose_index")
        self.assertEqual(1.0, uses_loose_index.value)

    def test_루프를_포함한_전체_방문_행을_계산한다(self):
        text = """
        -> Covering index lookup on c using idx_comment_post_status_created
           (actual time=0.001..0.003 rows=100 loops=1000)
        """
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "before.txt"
            path.write_text(text, encoding="utf-8")

            rows = measurements(path, "v1", "comments=100000", "2026-08-03T00:00:00+09:00")

        actual_rows = next(row for row in rows if row.metric == "actual_rows")
        self.assertEqual(100000.0, actual_rows.value)


if __name__ == "__main__":
    unittest.main()
