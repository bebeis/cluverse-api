package cluverse.board.service;

import cluverse.board.domain.Board;
import cluverse.board.exception.BoardExceptionMessage;
import cluverse.board.service.implement.BoardReader;
import cluverse.board.service.implement.BoardWriter;
import cluverse.board.service.request.BoardCreateRequest;
import cluverse.board.service.request.BoardSearchRequest;
import cluverse.board.service.request.BoardUpdateRequest;
import cluverse.board.service.response.BoardAdminResponse;
import cluverse.board.service.response.BoardDetailResponse;
import cluverse.board.service.response.BoardDirectoryResponse;
import cluverse.board.service.response.BoardHomeResponse;
import cluverse.common.exception.ForbiddenException;
import cluverse.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardReader boardReader;
    private final BoardWriter boardWriter;
    private final MemberService memberService;

    public BoardDirectoryResponse getBoardDirectory(Long memberId, BoardSearchRequest request) {
        return boardReader.readBoardDirectory(memberId, memberService.isVerified(memberId), request);
    }

    public BoardDetailResponse getBoard(Long memberId, Long boardId) {
        return boardReader.readBoardDetail(memberId, memberService.isVerified(memberId), boardId);
    }

    public BoardHomeResponse getBoardHome(Long memberId, Long boardId) {
        return boardReader.readBoardHome(memberId, memberService.isVerified(memberId), boardId);
    }

    public void validateReadableBoard(Long memberId, Long boardId) {
        boardReader.validateReadable(memberId, boardId);
    }

    public void validateWritableBoard(Long memberId, Long boardId) {
        boardReader.validateWritable(memberId, memberService.isVerified(memberId), boardId);
    }

    @Transactional
    public Board createGroupBoard(String name, String description) {
        return boardWriter.createGroupBoard(name, description);
    }

    @Transactional
    public void updateGroupBoard(Long boardId, String name, String description) {
        Board board = boardReader.readOrThrow(boardId);
        boardWriter.updateGroupBoard(board, name, description);
    }

    @Transactional
    public void deactivateGroupBoard(Long boardId) {
        Board board = boardReader.readOrThrow(boardId);
        boardWriter.deactivateGroupBoard(board);
    }

    @Transactional
    public BoardAdminResponse createBoard(Long memberId, BoardCreateRequest request) {
        validateAdmin(memberId);
        Board board = boardWriter.create(request);
        return BoardAdminResponse.from(board);
    }

    @Transactional
    public BoardAdminResponse updateBoard(Long memberId, Long boardId, BoardUpdateRequest request) {
        validateAdmin(memberId);
        Board board = boardReader.readOrThrow(boardId);
        boardWriter.update(board, request);
        return BoardAdminResponse.from(board);
    }

    @Transactional
    public void deleteBoard(Long memberId, Long boardId) {
        validateAdmin(memberId);
        Board board = boardReader.readOrThrow(boardId);
        boardWriter.delete(board);
    }

    private void validateAdmin(Long memberId) {
        if (!memberService.isAdmin(memberId)) {
            throw new ForbiddenException(BoardExceptionMessage.BOARD_ACCESS_DENIED.getMessage());
        }
    }
}
