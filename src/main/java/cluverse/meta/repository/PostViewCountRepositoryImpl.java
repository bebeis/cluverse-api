package cluverse.meta.repository;

import cluverse.meta.repository.dto.ViewCountDelta;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.OptionalLong;

@RequiredArgsConstructor
public class PostViewCountRepositoryImpl implements PostViewCountRepositoryCustom {

    // 전체 delta를 단일 배치로 보내면 드라이버가 파라미터 전부를 쥔다 — 배치 크기 상한
    private static final int MAX_BATCH_SIZE = 1_000;

    private final JdbcTemplate jdbcTemplate;

    // LAST_INSERT_ID(expr)는 커넥션 단위 값 — UPDATE와 SELECT를 같은 커넥션에서 실행해야 한다
    @Override
    public OptionalLong increaseAndGet(Long postId) {
        return jdbcTemplate.execute((ConnectionCallback<OptionalLong>) connection -> {
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE post_view_count
                    SET view_count = LAST_INSERT_ID(view_count + 1),
                        updated_at = NOW()
                    WHERE post_id = ?
                    """)) {
                update.setLong(1, postId);
                if (update.executeUpdate() == 0) {
                    return OptionalLong.empty();
                }
            }
            try (PreparedStatement select = connection.prepareStatement("SELECT LAST_INSERT_ID()");
                 ResultSet resultSet = select.executeQuery()) {
                resultSet.next();
                return OptionalLong.of(resultSet.getLong(1));
            }
        });
    }

    @Override
    public void increaseByDeltas(List<ViewCountDelta> deltas) {
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
}
