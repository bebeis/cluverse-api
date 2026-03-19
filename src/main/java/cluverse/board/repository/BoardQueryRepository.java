package cluverse.board.repository;

import cluverse.board.domain.BoardType;
import cluverse.board.repository.dto.BoardGroupQueryDto;
import cluverse.board.repository.dto.BoardQueryDto;
import cluverse.board.domain.QBoard;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cluverse.board.domain.QBoard.board;
import static cluverse.group.domain.QGroup.group;
import static cluverse.group.domain.QGroupMember.groupMember;

@Repository
@RequiredArgsConstructor
public class BoardQueryRepository {

    private static final QBoard childBoard = new QBoard("childBoard");
    private static final long UNKNOWN_MEMBER_ID = -1L;

    private final JPAQueryFactory queryFactory;

    public List<BoardQueryDto> findBoardSummaries(BoardType boardType, boolean activeOnly) {
        return queryFactory
                .select(Projections.constructor(
                        BoardQueryDto.class,
                        board.id,
                        board.boardType,
                        board.name,
                        board.description,
                        board.parentId,
                        board.depth,
                        board.displayOrder,
                        board.isActive,
                        JPAExpressions.select(childBoard.count())
                                .from(childBoard)
                                .where(
                                        childBoard.parentId.eq(board.id),
                                        activeOnly ? childBoard.isActive.isTrue() : null
                                )
                ))
                .from(board)
                .where(
                        boardTypeEq(boardType),
                        activeOnly ? board.isActive.isTrue() : null
                )
                .orderBy(board.depth.asc(), board.displayOrder.asc(), board.id.asc())
                .fetch();
    }

    public List<BoardQueryDto> findChildBoardSummaries(Long parentBoardId, boolean activeOnly) {
        return queryFactory
                .select(Projections.constructor(
                        BoardQueryDto.class,
                        board.id,
                        board.boardType,
                        board.name,
                        board.description,
                        board.parentId,
                        board.depth,
                        board.displayOrder,
                        board.isActive,
                        JPAExpressions.select(childBoard.count())
                                .from(childBoard)
                                .where(
                                        childBoard.parentId.eq(board.id),
                                        activeOnly ? childBoard.isActive.isTrue() : null
                                )
                ))
                .from(board)
                .where(
                        board.parentId.eq(parentBoardId),
                        activeOnly ? board.isActive.isTrue() : null
                )
                .orderBy(board.displayOrder.asc(), board.id.asc())
                .fetch();
    }

    public Map<Long, BoardGroupQueryDto> findGroupBoardMap(Collection<Long> boardIds, Long memberId) {
        if (boardIds == null || boardIds.isEmpty()) {
            return Map.of();
        }

        List<BoardGroupQueryDto> rows = queryFactory
                .select(Projections.constructor(
                        BoardGroupQueryDto.class,
                        group.boardId,
                        group.id,
                        group.name,
                        group.visibility,
                        group.status,
                        groupMember.role
                ))
                .from(group)
                .leftJoin(groupMember).on(
                        groupMember.group.id.eq(group.id),
                        groupMember.memberId.eq(resolveMemberId(memberId))
                )
                .where(group.boardId.in(boardIds))
                .fetch();

        Map<Long, BoardGroupQueryDto> result = new LinkedHashMap<>();
        for (BoardGroupQueryDto row : rows) {
            result.put(row.boardId(), row);
        }
        return result;
    }

    private BooleanExpression boardTypeEq(BoardType boardType) {
        return boardType == null ? null : board.boardType.eq(boardType);
    }

    private Long resolveMemberId(Long memberId) {
        return memberId == null ? UNKNOWN_MEMBER_ID : memberId;
    }
}
