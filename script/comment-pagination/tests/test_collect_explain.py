from __future__ import annotations

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from collect_explain import extract_node


class CollectExplainTest(unittest.TestCase):

    def test_재귀_CTE의_구체화_방문_행을_읽는다(self):
        explain = (
            "-> Table scan on comment_tree  (cost=1 rows=10) "
            "(actual time=0.5..0.6 rows=1000 loops=1)"
        )

        self.assertEqual((1000.0, 1.0), extract_node(explain, "v1"))

    def test_path_인덱스의_범위_방문_행을_읽는다(self):
        explain = (
            "-> Index range scan on comment using idx_comment_post_path "
            "(actual time=0.1..0.2 rows=101 loops=1)"
        )

        self.assertEqual((101.0, 1.0), extract_node(explain, "v2"))


if __name__ == "__main__":
    unittest.main()
