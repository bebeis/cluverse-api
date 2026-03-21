package cluverse.comment.repository;

import cluverse.comment.domain.CommentStatus;
import cluverse.comment.exception.CommentExceptionMessage;
import cluverse.comment.repository.dto.CommentPageQueryResult;
import cluverse.comment.repository.dto.CommentQueryDto;
import cluverse.comment.service.request.CommentPageRequest;
import cluverse.comment.service.response.CommentLastRepliedPost;
import cluverse.common.exception.NotFoundException;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import static cluverse.comment.domain.QComment.comment;

@Repository
@RequiredArgsConstructor
public class CommentQueryRepository {

    private static final int MAX_DEPTH = 5;

    private final JPAQueryFactory queryFactory;
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public CommentPageQueryResult findCommentPage(Long viewerId, CommentPageRequest request) {
        List<Long> rootCommentIds = findPagedRootCommentIds(request.postId(), request.parentCommentId(),
                request.offset(), request.limit() + 1);

        boolean hasNext = rootCommentIds.size() > request.limit();
        if (hasNext) {
            rootCommentIds = rootCommentIds.subList(0, request.limit());
        }

        return new CommentPageQueryResult(readCommentTree(viewerId, rootCommentIds), hasNext);
    }

    public CommentQueryDto findComment(Long viewerId, Long commentId) {
        List<CommentQueryDto> comments = readCommentTree(viewerId, List.of(commentId));
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

    private List<Long> findPagedRootCommentIds(Long postId, Long parentCommentId, int offset, int limit) {
        return queryFactory.select(comment.id)
                .from(comment)
                .where(
                        comment.postId.eq(postId),
                        parentIdEq(parentCommentId)
                )
                .orderBy(comment.createdAt.asc(), comment.id.asc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    private BooleanExpression parentIdEq(Long parentCommentId) {
        return parentCommentId == null ? comment.parentId.isNull() : comment.parentId.eq(parentCommentId);
    }

    private List<CommentQueryDto> readCommentTree(Long viewerId, List<Long> rootCommentIds) {
        if (rootCommentIds.isEmpty()) {
            return List.of();
        }

        String sql = """
                WITH RECURSIVE comment_tree (
                    comment_id,
                    post_id,
                    member_id,
                    parent_id,
                    depth,
                    content,
                    status,
                    is_anonymous,
                    like_count,
                    reply_count,
                    created_at,
                    updated_at,
                    root_created_at,
                    root_comment_id,
                    sort_path
                ) AS (
                    SELECT
                        c.comment_id,
                        c.post_id,
                        c.member_id,
                        c.parent_id,
                        c.depth,
                        c.content,
                        c.status,
                        c.is_anonymous,
                        c.like_count,
                        c.reply_count,
                        c.created_at,
                        c.updated_at,
                        c.created_at AS root_created_at,
                        c.comment_id AS root_comment_id,
                        CAST(LPAD(CAST(c.comment_id AS CHAR), 20, '0') AS CHAR(512)) AS sort_path
                    FROM comment c
                    WHERE c.comment_id IN (:rootCommentIds)

                    UNION ALL

                    SELECT
                        child.comment_id,
                        child.post_id,
                        child.member_id,
                        child.parent_id,
                        child.depth,
                        child.content,
                        child.status,
                        child.is_anonymous,
                        child.like_count,
                        child.reply_count,
                        child.created_at,
                        child.updated_at,
                        tree.root_created_at,
                        tree.root_comment_id,
                        CONCAT(tree.sort_path, '/', LPAD(CAST(child.comment_id AS CHAR), 20, '0')) AS sort_path
                    FROM comment child
                    JOIN comment_tree tree ON child.parent_id = tree.comment_id
                    WHERE child.depth <= :maxDepth
                )
                SELECT
                    tree.comment_id,
                    tree.post_id,
                    tree.parent_id,
                    tree.depth,
                    tree.content,
                    tree.status,
                    tree.is_anonymous,
                    tree.like_count,
                    tree.reply_count,
                    member.member_id AS author_member_id,
                    member.nickname AS author_nickname,
                    member_profile.profile_image_url AS author_profile_image_url,
                    CASE WHEN comment_like.comment_like_id IS NULL THEN FALSE ELSE TRUE END AS liked_by_me,
                    CASE WHEN block.block_id IS NULL THEN FALSE ELSE TRUE END AS blocked_author,
                    tree.created_at,
                    tree.updated_at
                FROM comment_tree tree
                JOIN member ON member.member_id = tree.member_id
                LEFT JOIN member_profile ON member_profile.member_id = member.member_id
                LEFT JOIN comment_like
                    ON comment_like.comment_id = tree.comment_id
                   AND comment_like.member_id = :viewerId
                LEFT JOIN block
                    ON block.blocker_id = :viewerId
                   AND block.blocked_id = tree.member_id
                ORDER BY tree.root_created_at ASC, tree.root_comment_id ASC, tree.sort_path ASC
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("rootCommentIds", rootCommentIds)
                .addValue("maxDepth", MAX_DEPTH)
                .addValue("viewerId", viewerId, Types.BIGINT);

        return namedParameterJdbcTemplate.query(sql, parameters, commentRowMapper());
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
}
