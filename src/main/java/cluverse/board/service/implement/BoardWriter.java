package cluverse.board.service.implement;

import cluverse.board.domain.Board;
import cluverse.board.domain.BoardType;
import cluverse.board.exception.BoardExceptionMessage;
import cluverse.board.repository.BoardRepository;
import cluverse.board.service.request.BoardCreateRequest;
import cluverse.board.service.request.BoardUpdateRequest;
import cluverse.common.exception.BadRequestException;
import cluverse.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class BoardWriter {

    private static final int MAX_DEPTH = 2;

    private final BoardRepository boardRepository;

    public Board create(BoardCreateRequest request) {
        validateCreatableType(request.boardType());

        Board parentBoard = findParentBoard(request.parentBoardId());
        validateParentType(parentBoard, request.boardType());
        int depth = calculateDepth(parentBoard);

        Board board = Board.create(
                request.boardType(),
                request.name().trim(),
                request.description(),
                parentBoard == null ? null : parentBoard.getId(),
                depth,
                request.displayOrderOrDefault(),
                request.isActiveOrDefault()
        );

        return boardRepository.save(board);
    }

    public Board createGroupBoard(String name, String description) {
        return boardRepository.save(Board.createGroupBoard(name, description));
    }

    public void update(Board board, BoardUpdateRequest request) {
        validateMutable(board);
        board.update(
                request.name().trim(),
                request.description(),
                request.displayOrderOrDefault(),
                request.isActiveOrDefault()
        );
    }

    public void updateGroupBoard(Board board, String name, String description) {
        validateGroupBoard(board);
        board.updateGroupMetadata(name.trim(), description);
    }

    public void delete(Board board) {
        validateMutable(board);
        validateDeletable(board);
        board.deactivate();
    }

    public void deactivateGroupBoard(Board board) {
        validateGroupBoard(board);
        board.deactivate();
    }

    private Board findParentBoard(Long parentBoardId) {
        if (parentBoardId == null) {
            return null;
        }
        return boardRepository.findById(parentBoardId)
                .orElseThrow(() -> new NotFoundException(BoardExceptionMessage.BOARD_PARENT_NOT_FOUND.getMessage()));
    }

    private void validateCreatableType(BoardType boardType) {
        if (boardType == BoardType.GROUP) {
            throw new BadRequestException(BoardExceptionMessage.BOARD_GROUP_TYPE_NOT_SUPPORTED.getMessage());
        }
    }

    private void validateMutable(Board board) {
        if (board.getBoardType() == BoardType.GROUP) {
            throw new BadRequestException(BoardExceptionMessage.BOARD_GROUP_TYPE_NOT_SUPPORTED.getMessage());
        }
    }

    private void validateGroupBoard(Board board) {
        if (board.getBoardType() != BoardType.GROUP) {
            throw new BadRequestException(BoardExceptionMessage.BOARD_GROUP_TYPE_REQUIRED.getMessage());
        }
    }

    private void validateParentType(Board parentBoard, BoardType boardType) {
        if (parentBoard != null && parentBoard.getBoardType() != boardType) {
            throw new BadRequestException(BoardExceptionMessage.BOARD_PARENT_TYPE_MISMATCH.getMessage());
        }
    }

    private int calculateDepth(Board parentBoard) {
        if (parentBoard == null) {
            return 0;
        }
        int depth = parentBoard.getDepth() + 1;
        if (depth > MAX_DEPTH) {
            throw new BadRequestException(BoardExceptionMessage.BOARD_DEPTH_EXCEEDED.getMessage());
        }
        return depth;
    }

    private void validateDeletable(Board board) {
        if (boardRepository.existsByParentIdAndIsActiveTrue(board.getId())) {
            throw new BadRequestException(BoardExceptionMessage.BOARD_HAS_ACTIVE_CHILDREN.getMessage());
        }
    }
}
