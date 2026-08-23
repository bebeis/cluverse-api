package cluverse.comment.repository;

import cluverse.comment.domain.CommentPageCursor;
import cluverse.comment.domain.CommentStatus;
import cluverse.comment.exception.CommentExceptionMessage;
import cluverse.comment.repository.dto.CommentPageQueryResult;
import cluverse.comment.repository.dto.CommentQueryDto;
import cluverse.comment.service.request.CommentPageRequest;
import cluverse.comment.service.response.CommentLastRepliedPost;
import cluverse.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class CommentQueryRepository {

    private static final int MAX_DEPTH = 5;

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public long findMaxCommentId() {
        Long maxCommentId = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(comment_id), 0) FROM comment",
                Long.class
        );
        return maxCommentId == null ? 0L : maxCommentId;
    }

    public CommentPageQueryResult findCommentPage(Long viewerId, CommentPageRequest request,
                                                  CommentPageCursor cursor) {
        List<CommentPageEntry> entries = findSiblingPageEntries(request, cursor, request.limit() + 1);
        return createPageResult(viewerId, entries, request.limit());
    }

    public CommentPageQueryResult findCommentThreadPage(
            Long viewerId,
            Long postId,
            Long rootCommentId,
            CommentPageCursor cursor,
            int limit
    ) {
        List<CommentPageEntry> entries = findRecursiveThreadEntries(
                postId, rootCommentId, cursor, limit + 1);
        return createPageResult(viewerId, entries, limit);
    }

    public CommentQueryDto findComment(Long viewerId, Long commentId) {
        List<CommentQueryDto> comments = readComments(viewerId, List.of(commentId));
        if (comments.isEmpty()) {
            throw new NotFoundException(CommentExceptionMessage.COMMENT_NOT_FOUND.getMessage());
        }
        return comments.getFirst();
    }

    public List<CommentLastRepliedPost> findRecentCommentRepliedPosts(Long size) {
        String sql = """
                SELECT x.post_id, x.last_commented_at
                FROM (
                    SELECT c.post_id, MAX(c.created_at) AS last_commented_at
                    FROM comment c
                    GROUP BY c.post_id
                ) x
                ORDER BY x.last_commented_at DESC, x.post_id DESC
                LIMIT ?
                """;

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new CommentLastRepliedPost(
                resultSet.getLong("post_id"),
                resultSet.getTimestamp("last_commented_at").toLocalDateTime()
        ), size);
    }

    private List<CommentPageEntry> findSiblingPageEntries(
            CommentPageRequest request,
            CommentPageCursor cursor,
            int pageSize
    ) {
        String parentCondition = request.parentCommentId() == null
                ? "c.parent_id IS NULL"
                : "c.parent_id = :parentCommentId";
        String cursorCondition = cursor.hasPath() ? "AND " + pathSegment("c") + " > :cursorPath" : "";
        String sql = """
                SELECT c.comment_id, %s AS sort_path
                FROM comment c
                WHERE c.post_id = :postId
                  AND %s
                  AND c.created_at <= :asOf
                  AND c.comment_id <= :snapshotMaxCommentId
                  %s
                ORDER BY c.created_at, c.comment_id
                LIMIT :pageSize
                """.formatted(pathSegment("c"), parentCondition, cursorCondition);

        return namedParameterJdbcTemplate.query(
                sql, pageParameters(request, cursor, pageSize), pageEntryRowMapper());
    }

    private List<CommentPageEntry> findRecursiveThreadEntries(
            Long postId,
            Long rootCommentId,
            CommentPageCursor cursor,
            int pageSize
    ) {
        String cursorCondition = cursor.hasPath() ? "WHERE sort_path > :cursorPath" : "";
        String sql = """
                WITH RECURSIVE comment_tree (comment_id, depth, sort_path) AS (
                    SELECT c.comment_id, c.depth, CAST(%s AS CHAR(255))
                    FROM comment c
                    WHERE c.post_id = :postId
                      AND c.comment_id = :rootCommentId
                      AND c.created_at <= :asOf
                      AND c.comment_id <= :snapshotMaxCommentId

                    UNION ALL

                    SELECT child.comment_id, child.depth,
                           CONCAT(RTRIM(parent.sort_path), '/', %s)
                    FROM comment child
                    JOIN comment_tree parent ON child.parent_id = parent.comment_id
                    WHERE child.post_id = :postId
                      AND child.depth <= :maxDepth
                      AND child.created_at <= :asOf
                      AND child.comment_id <= :snapshotMaxCommentId
                )
                SELECT comment_id, sort_path
                FROM comment_tree
                %s
                ORDER BY sort_path
                LIMIT :pageSize
                """.formatted(pathSegment("c"), pathSegment("child"), cursorCondition);

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("postId", postId)
                .addValue("rootCommentId", rootCommentId)
                .addValue("cursorPath", cursor.path())
                .addValue("asOf", cursor.asOf())
                .addValue("snapshotMaxCommentId", cursor.snapshotMaxCommentId())
                .addValue("maxDepth", MAX_DEPTH)
                .addValue("pageSize", pageSize);
        return namedParameterJdbcTemplate.query(sql, parameters, pageEntryRowMapper());
    }

    private String pathSegment(String alias) {
        return "CONCAT(DATE_FORMAT(" + alias + ".created_at, '%Y%m%d%H%i%s%f'), '-', "
                + "LPAD(RTRIM(CAST(" + alias + ".comment_id AS CHAR(20))), 20, '0'))";
    }

    private MapSqlParameterSource pageParameters(CommentPageRequest request,
                                                  CommentPageCursor cursor,
                                                  int pageSize) {
        return new MapSqlParameterSource()
                .addValue("postId", request.postId())
                .addValue("parentCommentId", request.parentCommentId(), Types.BIGINT)
                .addValue("cursorPath", cursor.path())
                .addValue("asOf", cursor.asOf())
                .addValue("snapshotMaxCommentId", cursor.snapshotMaxCommentId())
                .addValue("pageSize", pageSize);
    }

    private CommentPageQueryResult createPageResult(Long viewerId,
                                                     List<CommentPageEntry> entries,
                                                     int limit) {
        boolean hasNext = entries.size() > limit;
        List<CommentPageEntry> selectedEntries = hasNext ? entries.subList(0, limit) : entries;
        List<Long> commentIds = selectedEntries.stream()
                .map(CommentPageEntry::commentId)
                .toList();
        Map<Long, CommentQueryDto> commentsById = new LinkedHashMap<>();
        readComments(viewerId, commentIds).forEach(comment -> commentsById.put(comment.commentId(), comment));
        List<CommentQueryDto> comments = commentIds.stream()
                .map(commentsById::get)
                .filter(Objects::nonNull)
                .toList();
        String lastPath = selectedEntries.isEmpty() ? null : selectedEntries.getLast().sortPath();
        return new CommentPageQueryResult(comments, hasNext, lastPath);
    }

    private List<CommentQueryDto> readComments(Long viewerId, List<Long> commentIds) {
        if (commentIds.isEmpty()) {
            return List.of();
        }

        String sql = """
                SELECT
                    comment.comment_id,
                    comment.post_id,
                    comment.parent_id,
                    comment.depth,
                    comment.content,
                    comment.status,
                    comment.is_anonymous,
                    comment.like_count,
                    comment.reply_count,
                    member.member_id AS author_member_id,
                    member.nickname AS author_nickname,
                    member_profile.profile_image_url AS author_profile_image_url,
                    CASE WHEN comment_like.comment_like_id IS NULL THEN FALSE ELSE TRUE END AS liked_by_me,
                    CASE WHEN block.block_id IS NULL THEN FALSE ELSE TRUE END AS blocked_author,
                    comment.created_at,
                    comment.updated_at
                FROM comment
                JOIN member ON member.member_id = comment.member_id
                LEFT JOIN member_profile ON member_profile.member_id = member.member_id
                LEFT JOIN comment_like
                    ON comment_like.comment_id = comment.comment_id
                   AND comment_like.member_id = :viewerId
                LEFT JOIN block
                    ON block.blocker_id = :viewerId
                   AND block.blocked_id = comment.member_id
                WHERE comment.comment_id IN (:commentIds)
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("commentIds", commentIds)
                .addValue("viewerId", viewerId, Types.BIGINT);
        return namedParameterJdbcTemplate.query(sql, parameters, commentRowMapper());
    }

    private RowMapper<CommentPageEntry> pageEntryRowMapper() {
        return (resultSet, rowNum) -> new CommentPageEntry(
                resultSet.getLong("comment_id"),
                resultSet.getString("sort_path")
        );
    }

    private RowMapper<CommentQueryDto> commentRowMapper() {
        return (resultSet, rowNum) -> new CommentQueryDto(
                resultSet.getLong("comment_id"),
                resultSet.getLong("post_id"),
                getLong(resultSet, "parent_id"),
                resultSet.getInt("depth"),
                resultSet.getString("content"),
                CommentStatus.valueOf(resultSet.getString("status")),
                resultSet.getBoolean("is_anonymous"),
                resultSet.getLong("like_count"),
                resultSet.getLong("reply_count"),
                resultSet.getLong("author_member_id"),
                resultSet.getString("author_nickname"),
                resultSet.getString("author_profile_image_url"),
                resultSet.getBoolean("liked_by_me"),
                resultSet.getBoolean("blocked_author"),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    private Long getLong(ResultSet resultSet, String columnLabel) throws SQLException {
        long value = resultSet.getLong(columnLabel);
        return resultSet.wasNull() ? null : value;
    }

    private record CommentPageEntry(Long commentId, String sortPath) {
    }
}
