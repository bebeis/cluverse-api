package cluverse.meta.repository;

import cluverse.meta.repository.dto.ViewCountDelta;
import cluverse.meta.repository.dto.ViewCountSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@RequiredArgsConstructor
public class PostViewCountRepositoryImpl implements PostViewCountRepositoryCustom {

    // 전체 delta를 단일 배치로 보내면 드라이버가 파라미터 전부를 쥔다 — 배치 크기 상한
    private static final int MAX_BATCH_SIZE = 1_000;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void increaseByDeltas(List<ViewCountDelta> deltas) {
        if (deltas.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(
                "UPDATE post_view_count SET view_count = view_count + ?, updated_at = NOW() WHERE post_id = ?",
                deltas,
                Math.min(deltas.size(), MAX_BATCH_SIZE),
                (statement, delta) -> {
                    statement.setLong(1, delta.delta());
                    statement.setLong(2, delta.postId());
                }
        );
    }

    @Override
    public void checkpointViewCounts(List<ViewCountSnapshot> snapshots) {
        if (snapshots.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(
                """
                UPDATE post_view_count
                SET view_count = GREATEST(view_count, ?), updated_at = NOW()
                WHERE post_id = ?
                """,
                snapshots,
                Math.min(snapshots.size(), MAX_BATCH_SIZE),
                (statement, snapshot) -> {
                    statement.setLong(1, snapshot.viewCount());
                    statement.setLong(2, snapshot.postId());
                }
        );
    }
}
