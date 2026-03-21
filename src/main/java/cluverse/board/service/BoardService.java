package cluverse.board.service;

import cluverse.board.domain.Board;
import cluverse.board.exception.BoardExceptionMessage;
import cluverse.board.service.implement.BoardReader;
import cluverse.board.service.implement.BoardWriter;
import cluverse.board.service.request.BoardCreateRequest;
import cluverse.board.service.request.BoardUpdateRequest;
import cluverse.board.service.response.BoardAdminResponse;
import cluverse.common.exception.ForbiddenException;
import cluverse.member.service.implement.MemberReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardService {

    private final BoardReader boardReader;
    private final BoardWriter boardWriter;
    private final MemberReader memberReader;

    public Board createGroupBoard(String name, String description) {
        return boardWriter.createGroupBoard(name, description);
    }

    public void updateGroupBoard(Long boardId, String name, String description) {
        Board board = boardReader.readOrThrow(boardId);
        boardWriter.updateGroupBoard(board, name, description);
    }

    public void deactivateGroupBoard(Long boardId) {
        Board board = boardReader.readOrThrow(boardId);
        boardWriter.deactivateGroupBoard(board);
    }

    public BoardAdminResponse createBoard(Long memberId, BoardCreateRequest request) {
        validateAdmin(memberId);
        Board board = boardWriter.create(request);
        return BoardAdminResponse.from(board);
    }

    public BoardAdminResponse updateBoard(Long memberId, Long boardId, BoardUpdateRequest request) {
        validateAdmin(memberId);
        Board board = boardReader.readOrThrow(boardId);
        boardWriter.update(board, request);
        return BoardAdminResponse.from(board);
    }

    public void deleteBoard(Long memberId, Long boardId) {
        validateAdmin(memberId);
        Board board = boardReader.readOrThrow(boardId);
        boardWriter.delete(board);
    }

    private void validateAdmin(Long memberId) {
        if (!memberReader.isAdmin(memberId)) {
            throw new ForbiddenException(BoardExceptionMessage.BOARD_ACCESS_DENIED.getMessage());
        }
    }
}
