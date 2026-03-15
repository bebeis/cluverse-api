package cluverse.board.service.implement;

import cluverse.board.domain.Board;
import cluverse.board.domain.BoardType;
import cluverse.board.exception.BoardExceptionMessage;
import cluverse.board.repository.BoardQueryRepository;
import cluverse.board.repository.BoardRepository;
import cluverse.board.repository.dto.BoardGroupQueryDto;
import cluverse.board.repository.dto.BoardQueryDto;
import cluverse.board.service.request.BoardSearchRequest;
import cluverse.board.service.response.BoardBreadcrumbResponse;
import cluverse.board.service.response.BoardDetailResponse;
import cluverse.board.service.response.BoardDirectoryResponse;
import cluverse.board.service.response.BoardHomeResponse;
import cluverse.board.service.response.BoardHomeTabResponse;
import cluverse.board.service.response.BoardHomeTabType;
import cluverse.board.service.response.BoardPostingPolicyResponse;
import cluverse.board.service.response.BoardSortOption;
import cluverse.board.service.response.BoardSummaryResponse;
import cluverse.board.service.response.GroupBoardSummaryResponse;
import cluverse.common.exception.ForbiddenException;
import cluverse.common.exception.NotFoundException;
import cluverse.group.domain.GroupMemberRole;
import cluverse.group.domain.GroupVisibility;
import cluverse.post.domain.PostCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardReader {

    private static final int MAX_DEPTH = 2;
    private static final String COMMUNITY_EXTERNAL_RULE = "학과/관심사 보드는 외부 공개 설정 없이 전체 공개됩니다.";
    private static final String COMMUNITY_WRITE_RULE = "인증 완료 회원만 작성할 수 있습니다.";
    private static final String GROUP_EXTERNAL_RULE = "PUBLIC 그룹일 때만 외부 공개를 허용합니다.";
    private static final String GROUP_WRITE_RULE = "공지 작성은 관리자, 일반 글 작성은 그룹 멤버가 가능합니다.";

    private final BoardRepository boardRepository;
    private final BoardQueryRepository boardQueryRepository;

    public Board readOrThrow(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new NotFoundException(BoardExceptionMessage.BOARD_NOT_FOUND.getMessage()));
    }

    public BoardDirectoryResponse readBoardDirectory(Long memberId,
                                                     boolean verified,
                                                     BoardSearchRequest request) {
        Board parentBoard = request.parentBoardId() == null ? null : readOrThrow(request.parentBoardId());
        BoardType boardType = resolveBoardType(parentBoard, request.type());
        boolean activeOnly = request.activeOnlyOrDefault();

        validateDirectoryReadable(memberId, parentBoard, boardType);

        List<BoardQueryDto> allBoards = boardQueryRepository.findBoardSummaries(boardType, activeOnly);
        List<BoardQueryDto> scopedBoards = filterScopedBoards(allBoards, parentBoard, request.depthOrDefault());
        List<BoardQueryDto> filteredBoards = filterKeyword(scopedBoards, request.keyword());
        Map<Long, BoardGroupQueryDto> groupBoardMap = readGroupBoardMap(filteredBoards, memberId);
        List<BoardQueryDto> readableBoards = filterReadableBoards(filteredBoards, groupBoardMap);

        return new BoardDirectoryResponse(
                boardType,
                request.parentBoardId(),
                request.depthOrDefault(),
                activeOnly,
                readableBoards.stream()
                        .map(board -> toBoardSummaryResponse(board, verified, groupBoardMap.get(board.boardId())))
                        .toList()
        );
    }

    public BoardDetailResponse readBoardDetail(Long memberId, boolean verified, Long boardId) {
        Board board = readOrThrow(boardId);
        BoardGroupQueryDto groupBoard = readGroupBoard(board, memberId);
        validateReadable(board.getBoardType(), groupBoard);

        return new BoardDetailResponse(
                board.getId(),
                board.getBoardType(),
                board.getName(),
                board.getDescription(),
                board.getParentId(),
                board.getDepth(),
                board.getDisplayOrder(),
                board.isActive(),
                isReadable(board.getBoardType(), groupBoard),
                isWritable(board.getBoardType(), verified, groupBoard),
                isManageable(board.getBoardType(), groupBoard),
                isMemberOnly(board.getBoardType()),
                createPostingPolicy(board.getBoardType()),
                readBreadcrumbs(board),
                readChildSummaries(memberId, verified, board.getId()),
                toGroupBoardSummary(groupBoard)
        );
    }

    public BoardHomeResponse readBoardHome(Long memberId, boolean verified, Long boardId) {
        Board board = readOrThrow(boardId);
        BoardGroupQueryDto groupBoard = readGroupBoard(board, memberId);
        validateReadable(board.getBoardType(), groupBoard);

        return new BoardHomeResponse(
                board.getId(),
                board.getBoardType(),
                board.getName(),
                board.getDescription(),
                isMemberOnly(board.getBoardType()),
                isReadable(board.getBoardType(), groupBoard),
                isWritable(board.getBoardType(), verified, groupBoard),
                isManageable(board.getBoardType(), groupBoard),
                isExternalVisible(board.getBoardType(), groupBoard),
                resolveDefaultTab(board.getBoardType()),
                createHomeTabs(board.getBoardType(), verified, groupBoard),
                createSupportedSorts(),
                createPostingPolicy(board.getBoardType())
        );
    }

    public void validateReadable(Long memberId, Long boardId) {
        Board board = readOrThrow(boardId);
        BoardGroupQueryDto groupBoard = readGroupBoard(board, memberId);
        validateReadable(board.getBoardType(), groupBoard);
    }

    public void validateWritable(Long memberId, boolean verified, Long boardId) {
        Board board = readOrThrow(boardId);
        BoardGroupQueryDto groupBoard = readGroupBoard(board, memberId);
        if (!isWritable(board.getBoardType(), verified, groupBoard)) {
            throw new ForbiddenException(BoardExceptionMessage.BOARD_WRITE_ACCESS_DENIED.getMessage());
        }
    }

    private BoardType resolveBoardType(Board parentBoard, BoardType requestedBoardType) {
        return parentBoard == null ? requestedBoardType : parentBoard.getBoardType();
    }

    private void validateDirectoryReadable(Long memberId, Board parentBoard, BoardType boardType) {
        if (parentBoard != null && parentBoard.getBoardType() == BoardType.GROUP) {
            validateReadable(memberId, parentBoard.getId());
            return;
        }
        if (boardType == BoardType.GROUP && memberId == null) {
            throw new ForbiddenException(BoardExceptionMessage.BOARD_READ_ACCESS_DENIED.getMessage());
        }
    }

    private List<BoardQueryDto> filterScopedBoards(List<BoardQueryDto> boards, Board parentBoard, int requestedDepth) {
        int maxDepth = calculateMaxDepth(parentBoard, requestedDepth);
        Map<Long, BoardQueryDto> boardMap = boards.stream()
                .collect(Collectors.toMap(
                        BoardQueryDto::boardId,
                        board -> board,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        return boards.stream()
                .filter(board -> isScopedBoard(board, parentBoard, maxDepth, boardMap))
                .sorted(Comparator.comparingInt(BoardQueryDto::depth)
                        .thenComparingInt(BoardQueryDto::displayOrder)
                        .thenComparing(BoardQueryDto::boardId))
                .toList();
    }

    private int calculateMaxDepth(Board parentBoard, int requestedDepth) {
        if (parentBoard == null) {
            return Math.min(MAX_DEPTH, requestedDepth);
        }
        return Math.min(MAX_DEPTH, parentBoard.getDepth() + requestedDepth);
    }

    private boolean isScopedBoard(BoardQueryDto board,
                                  Board parentBoard,
                                  int maxDepth,
                                  Map<Long, BoardQueryDto> boardMap) {
        if (board.depth() > maxDepth) {
            return false;
        }
        if (parentBoard == null) {
            return true;
        }
        if (board.boardId().equals(parentBoard.getId())) {
            return false;
        }
        return isDescendantOf(board, parentBoard.getId(), boardMap);
    }

    private boolean isDescendantOf(BoardQueryDto board, Long ancestorBoardId, Map<Long, BoardQueryDto> boardMap) {
        Long currentParentId = board.parentBoardId();
        while (currentParentId != null) {
            if (currentParentId.equals(ancestorBoardId)) {
                return true;
            }
            BoardQueryDto parentBoard = boardMap.get(currentParentId);
            if (parentBoard == null) {
                return false;
            }
            currentParentId = parentBoard.parentBoardId();
        }
        return false;
    }

    private List<BoardQueryDto> filterKeyword(List<BoardQueryDto> boards, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return boards;
        }
        String normalizedKeyword = keyword.toLowerCase();
        return boards.stream()
                .filter(board -> board.name().toLowerCase().contains(normalizedKeyword))
                .toList();
    }

    private List<BoardQueryDto> filterReadableBoards(List<BoardQueryDto> boards,
                                                     Map<Long, BoardGroupQueryDto> groupBoardMap) {
        return boards.stream()
                .filter(board -> isReadable(board.boardType(), groupBoardMap.get(board.boardId())))
                .toList();
    }

    private Map<Long, BoardGroupQueryDto> readGroupBoardMap(Collection<BoardQueryDto> boards, Long memberId) {
        Set<Long> groupBoardIds = boards.stream()
                .filter(board -> board.boardType() == BoardType.GROUP)
                .map(BoardQueryDto::boardId)
                .collect(Collectors.toSet());
        return boardQueryRepository.findGroupBoardMap(groupBoardIds, memberId);
    }

    private BoardGroupQueryDto readGroupBoard(Board board, Long memberId) {
        if (board.getBoardType() != BoardType.GROUP) {
            return null;
        }
        return boardQueryRepository.findGroupBoardMap(Set.of(board.getId()), memberId).get(board.getId());
    }

    private List<BoardBreadcrumbResponse> readBreadcrumbs(Board board) {
        LinkedList<BoardBreadcrumbResponse> breadcrumbs = new LinkedList<>();
        Board currentBoard = board;
        while (currentBoard != null) {
            breadcrumbs.addFirst(new BoardBreadcrumbResponse(
                    currentBoard.getId(),
                    currentBoard.getName(),
                    currentBoard.getDepth()
            ));
            currentBoard = currentBoard.getParentId() == null
                    ? null
                    : boardRepository.findById(currentBoard.getParentId()).orElse(null);
        }
        return List.copyOf(breadcrumbs);
    }

    private List<BoardSummaryResponse> readChildSummaries(Long memberId, boolean verified, Long boardId) {
        List<BoardQueryDto> childBoards = boardQueryRepository.findChildBoardSummaries(boardId, true);
        Map<Long, BoardGroupQueryDto> groupBoardMap = readGroupBoardMap(childBoards, memberId);
        return childBoards.stream()
                .map(board -> toBoardSummaryResponse(board, verified, groupBoardMap.get(board.boardId())))
                .toList();
    }

    private BoardSummaryResponse toBoardSummaryResponse(BoardQueryDto board,
                                                        boolean verified,
                                                        BoardGroupQueryDto groupBoard) {
        return new BoardSummaryResponse(
                board.boardId(),
                board.boardType(),
                board.name(),
                board.description(),
                board.parentBoardId(),
                board.depth(),
                board.displayOrder(),
                board.isActive(),
                board.childCount(),
                isReadable(board.boardType(), groupBoard),
                isWritable(board.boardType(), verified, groupBoard),
                isMemberOnly(board.boardType())
        );
    }

    private GroupBoardSummaryResponse toGroupBoardSummary(BoardGroupQueryDto groupBoard) {
        if (groupBoard == null) {
            return null;
        }
        return new GroupBoardSummaryResponse(
                groupBoard.groupId(),
                groupBoard.groupName(),
                groupBoard.visibility(),
                groupBoard.isMember(),
                groupBoard.myRole()
        );
    }

    private boolean isReadable(BoardType boardType, BoardGroupQueryDto groupBoard) {
        if (boardType != BoardType.GROUP) {
            return true;
        }
        return groupBoard != null && groupBoard.isMember();
    }

    private void validateReadable(BoardType boardType, BoardGroupQueryDto groupBoard) {
        if (!isReadable(boardType, groupBoard)) {
            throw new ForbiddenException(BoardExceptionMessage.BOARD_READ_ACCESS_DENIED.getMessage());
        }
    }

    private boolean isWritable(BoardType boardType, boolean verified, BoardGroupQueryDto groupBoard) {
        if (boardType == BoardType.GROUP) {
            return groupBoard != null && groupBoard.isMember();
        }
        return verified;
    }

    private boolean isManageable(BoardType boardType, BoardGroupQueryDto groupBoard) {
        if (boardType != BoardType.GROUP || groupBoard == null) {
            return false;
        }
        return groupBoard.myRole() == GroupMemberRole.OWNER || groupBoard.myRole() == GroupMemberRole.ADMIN;
    }

    private boolean isMemberOnly(BoardType boardType) {
        return boardType == BoardType.GROUP;
    }

    private boolean isExternalVisible(BoardType boardType, BoardGroupQueryDto groupBoard) {
        if (boardType != BoardType.GROUP) {
            return true;
        }
        return groupBoard != null && groupBoard.visibility() == GroupVisibility.PUBLIC;
    }

    private BoardPostingPolicyResponse createPostingPolicy(BoardType boardType) {
        if (boardType == BoardType.GROUP) {
            return new BoardPostingPolicyResponse(
                    false,
                    true,
                    true,
                    GROUP_EXTERNAL_RULE,
                    GROUP_WRITE_RULE,
                    List.of(PostCategory.NOTICE, PostCategory.GENERAL)
            );
        }
        return new BoardPostingPolicyResponse(
                true,
                false,
                false,
                COMMUNITY_EXTERNAL_RULE,
                COMMUNITY_WRITE_RULE,
                List.of(
                        PostCategory.QUESTION,
                        PostCategory.INFORMATION,
                        PostCategory.REVIEW,
                        PostCategory.RESOURCE
                )
        );
    }

    private BoardHomeTabType resolveDefaultTab(BoardType boardType) {
        return boardType == BoardType.GROUP ? BoardHomeTabType.GENERAL : BoardHomeTabType.FEED;
    }

    private List<BoardHomeTabResponse> createHomeTabs(BoardType boardType,
                                                      boolean verified,
                                                      BoardGroupQueryDto groupBoard) {
        if (boardType == BoardType.GROUP) {
            return List.of(
                    new BoardHomeTabResponse(
                            BoardHomeTabType.NOTICE,
                            "공지",
                            PostCategory.NOTICE,
                            false,
                            true,
                            isManageable(boardType, groupBoard)
                    ),
                    new BoardHomeTabResponse(
                            BoardHomeTabType.GENERAL,
                            "일반",
                            PostCategory.GENERAL,
                            true,
                            true,
                            isWritable(boardType, verified, groupBoard)
                    )
            );
        }
        return List.of(
                new BoardHomeTabResponse(
                        BoardHomeTabType.FEED,
                        "피드",
                        null,
                        true,
                        true,
                        verified
                )
        );
    }

    private List<BoardSortOption> createSupportedSorts() {
        return List.of(
                BoardSortOption.LATEST,
                BoardSortOption.VIEW_COUNT,
                BoardSortOption.COMMENT_COUNT
        );
    }
}
