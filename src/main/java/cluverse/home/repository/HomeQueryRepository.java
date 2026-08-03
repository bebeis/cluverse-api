package cluverse.home.repository;

import cluverse.group.domain.GroupStatus;
import cluverse.home.repository.dto.AccessiblePostQueryResult;
import cluverse.home.repository.dto.HomeBoardQueryResult;
import cluverse.home.repository.dto.RecentCommentedPostCandidateQueryResult;
import cluverse.home.repository.dto.RecentCommentedPostQueryResult;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

import static cluverse.board.domain.QBoard.board;
import static cluverse.group.domain.QGroup.group;
import static cluverse.group.domain.QGroupMember.groupMember;
import static cluverse.interest.domain.QInterest.interest;
import static cluverse.major.domain.QMajor.major;
import static cluverse.member.domain.QMember.member;
import static cluverse.member.domain.QMemberMajor.memberMajor;

@Repository
@RequiredArgsConstructor
public class HomeQueryRepository {

    private static final String RECENT_POST_BASE_CONDITION = """
              AND p.status = 'ACTIVE'
              AND b.is_active = TRUE
              AND author.status = 'ACTIVE'
              AND NOT EXISTS (
                    SELECT 1
                    FROM block bl
                    WHERE bl.blocker_id = :memberId
                      AND bl.blocked_id = p.member_id
              )
              AND (
                    b.board_type <> 'GROUP'
                    OR b.board_id IN (:readableGroupBoardIds)
              )
            """;

    private final JPAQueryFactory queryFactory;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<HomeBoardQueryResult> findFavoriteBoards(
            Long memberId,
            Long cursorBoardId,
            int limit
    ) {
        return queryFactory
                .select(Projections.constructor(
                        HomeBoardQueryResult.class,
                        board.id,
                        board.boardType,
                        board.name,
                        board.parentId
                ))
                .from(board)
                .where(
                        board.isActive.isTrue(),
                        favoriteBoardCondition(memberId),
                        cursorAfter(cursorBoardId)
                )
                .orderBy(board.id.asc())
                .limit(limit)
                .fetch();
    }

    public List<RecentCommentedPostQueryResult> findRecentCommentedPostsV1(Long memberId, int limit) {
        String sql = """
                SELECT p.post_id, p.title, MAX(c.created_at) AS last_commented_at
                FROM comment c
                JOIN post p ON p.post_id = c.post_id
                JOIN board b ON b.board_id = p.board_id
                JOIN member author ON author.member_id = p.member_id
                WHERE c.status <> 'DELETED'
                """ + RECENT_POST_BASE_CONDITION + """
                GROUP BY p.post_id, p.title
                ORDER BY last_commented_at DESC, p.post_id DESC
                LIMIT :limit
                """;
        return readRecentCommentedPosts(sql, memberId, limit);
    }

    public List<RecentCommentedPostCandidateQueryResult> findRecentCommentedPostCandidatesV2(int limit) {
        String sql = """
                SELECT c.post_id, MAX(c.visible_created_at) AS last_commented_at
                FROM comment c
                GROUP BY c.post_id
                HAVING MAX(c.visible_created_at) IS NOT NULL
                ORDER BY last_commented_at DESC, c.post_id DESC
                LIMIT :limit
                """;
        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("limit", limit),
                (resultSet, rowNumber) -> new RecentCommentedPostCandidateQueryResult(
                        resultSet.getLong("post_id"),
                        resultSet.getTimestamp("last_commented_at").toLocalDateTime()
                )
        );
    }

    public List<AccessiblePostQueryResult> findAccessiblePostTitles(
            Long memberId,
            List<Long> candidatePostIds
    ) {
        String sql = """
                SELECT p.post_id, p.title
                FROM post p
                JOIN board b ON b.board_id = p.board_id
                JOIN member author ON author.member_id = p.member_id
                WHERE p.post_id IN (:candidatePostIds)
                """ + RECENT_POST_BASE_CONDITION;
        MapSqlParameterSource parameters = accessParameters(memberId)
                .addValue("candidatePostIds", candidatePostIds);
        return jdbcTemplate.query(sql, parameters, (resultSet, rowNumber) -> new AccessiblePostQueryResult(
                resultSet.getLong("post_id"),
                resultSet.getString("title")
        ));
    }

    public List<RecentCommentedPostQueryResult> findRecentCommentedPostsV2Fallback(Long memberId, int limit) {
        String sql = """
                SELECT p.post_id, p.title, latest.last_commented_at
                FROM (
                    SELECT c.post_id, MAX(c.visible_created_at) AS last_commented_at
                    FROM comment c
                    GROUP BY c.post_id
                    HAVING MAX(c.visible_created_at) IS NOT NULL
                ) latest
                JOIN post p ON p.post_id = latest.post_id
                JOIN board b ON b.board_id = p.board_id
                JOIN member author ON author.member_id = p.member_id
                WHERE 1 = 1
                """ + RECENT_POST_BASE_CONDITION + """
                ORDER BY latest.last_commented_at DESC, latest.post_id DESC
                LIMIT :limit
                """;
        return readRecentCommentedPosts(sql, memberId, limit);
    }

    public List<RecentCommentedPostQueryResult> findRecentCommentedPostsV3(Long memberId, int limit) {
        String sql = """
                SELECT /*+ JOIN_ORDER(activity, p, b, author) NO_BNL(p, b, author) */
                       p.post_id, p.title, activity.last_commented_at
                FROM post_comment_activity activity
                JOIN post p ON p.post_id = activity.post_id
                JOIN board b ON b.board_id = p.board_id
                JOIN member author ON author.member_id = p.member_id
                WHERE 1 = 1
                """ + RECENT_POST_BASE_CONDITION + """
                ORDER BY activity.last_commented_at DESC, activity.post_id DESC
                LIMIT :limit
                """;
        return readRecentCommentedPosts(sql, memberId, limit);
    }

    private List<RecentCommentedPostQueryResult> readRecentCommentedPosts(
            String sql,
            Long memberId,
            int limit
    ) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("memberId", memberId)
                .addValue("readableGroupBoardIds", findReadableGroupBoardIds(memberId))
                .addValue("limit", limit);
        return jdbcTemplate.query(sql, parameters, (resultSet, rowNumber) -> new RecentCommentedPostQueryResult(
                resultSet.getLong("post_id"),
                resultSet.getString("title"),
                resultSet.getTimestamp("last_commented_at").toLocalDateTime()
        ));
    }

    private MapSqlParameterSource accessParameters(Long memberId) {
        return new MapSqlParameterSource()
                .addValue("memberId", memberId)
                .addValue("readableGroupBoardIds", findReadableGroupBoardIds(memberId));
    }

    private List<Long> findReadableGroupBoardIds(Long memberId) {
        List<Long> boardIds = queryFactory.select(group.boardId)
                .from(groupMember)
                .join(groupMember.group, group)
                .where(
                        groupMember.memberId.eq(memberId),
                        group.status.eq(GroupStatus.ACTIVE)
                )
                .fetch();
        return boardIds.isEmpty() ? List.of(-1L) : boardIds;
    }

    private BooleanExpression favoriteBoardCondition(Long memberId) {
        NumberPath<Long> interestId = Expressions.numberPath(Long.class, "homeInterestId");
        return board.id.in(
                        JPAExpressions.select(major.boardId)
                                .from(memberMajor)
                                .join(major).on(major.id.eq(memberMajor.majorId))
                                .where(
                                        memberMajor.member.id.eq(memberId),
                                        major.isActive.isTrue()
                                )
                )
                .or(board.id.in(
                        JPAExpressions.select(interest.boardId)
                                .from(member)
                                .join(member.interests, interestId)
                                .join(interest).on(interest.id.eq(interestId))
                                .where(
                                        member.id.eq(memberId),
                                        interest.isActive.isTrue()
                                )
                ))
                .or(board.id.in(
                        JPAExpressions.select(group.boardId)
                                .from(groupMember)
                                .join(groupMember.group, group)
                                .where(
                                        groupMember.memberId.eq(memberId),
                                        group.status.eq(GroupStatus.ACTIVE)
                                )
                ));
    }

    private BooleanExpression cursorAfter(Long cursorBoardId) {
        return cursorBoardId == null ? null : board.id.gt(cursorBoardId);
    }
}
