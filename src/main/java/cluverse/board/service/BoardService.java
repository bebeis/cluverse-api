package cluverse.board.service;

import cluverse.board.domain.Board;
import cluverse.board.exception.BoardExceptionMessage;
import cluverse.board.service.implement.BoardWriter;
import cluverse.board.service.request.BoardCreateRequest;
import cluverse.board.service.request.BoardUpdateRequest;
import cluverse.board.service.response.BoardAdminResponse;
import cluverse.common.exception.ForbiddenException;
import cluverse.member.service.implement.MemberReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardWriter boardWriter;
    private final MemberReader memberReader;

    public BoardAdminResponse createBoard(Long memberId, BoardCreateRequest request) {
        validateAdmin(memberId);
        Board board = boardWriter.create(request);
        return BoardAdminResponse.from(board);
    }

    public BoardAdminResponse updateBoard(Long memberId, Long boardId, BoardUpdateRequest request) {
        validateAdmin(memberId);
        Board board = boardWriter.update(boardId, request);
        return BoardAdminResponse.from(board);
    }

    public void deleteBoard(Long memberId, Long boardId) {
        validateAdmin(memberId);
        boardWriter.delete(boardId);
    }

    private void validateAdmin(Long memberId) {
        if (!memberReader.isAdmin(memberId)) {
            throw new ForbiddenException(BoardExceptionMessage.BOARD_ACCESS_DENIED.getMessage());
        }
    }
}
