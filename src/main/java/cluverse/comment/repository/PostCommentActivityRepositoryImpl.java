package cluverse.comment.repository;

import cluverse.comment.domain.Comment;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;

@Repository
@RequiredArgsConstructor
public class PostCommentActivityRepositoryImpl implements PostCommentActivityRepositoryCustom {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public int upsertLatest(Comment comment) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("postId", comment.getPostId())
                .addValue("lastCommentId", comment.getId())
                .addValue(
                        "lastCommentedAt",
                        Timestamp.valueOf(comment.getCreatedAt().truncatedTo(ChronoUnit.SECONDS))
                );
        int updated = updateIfNewer(parameters);
        if (updated > 0) {
            return updated;
        }

        try {
            return insert(parameters);
        } catch (DuplicateKeyException exception) {
            return updateIfNewer(parameters);
        }
    }

    private int updateIfNewer(MapSqlParameterSource parameters) {
        String sql = """
                UPDATE post_comment_activity
                SET last_comment_id = :lastCommentId,
                    last_commented_at = :lastCommentedAt,
                    updated_at = CURRENT_TIMESTAMP
                WHERE post_id = :postId
                  AND (
                        last_commented_at < :lastCommentedAt
                        OR (
                            last_commented_at = :lastCommentedAt
                            AND last_comment_id < :lastCommentId
                        )
                  )
                """;
        return jdbcTemplate.update(sql, parameters);
    }

    private int insert(MapSqlParameterSource parameters) {
        String sql = """
                INSERT INTO post_comment_activity (
                    post_id, last_comment_id, last_commented_at, created_at, updated_at
                )
                VALUES (
                    :postId, :lastCommentId, :lastCommentedAt, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """;
        return jdbcTemplate.update(sql, parameters);
    }
}
