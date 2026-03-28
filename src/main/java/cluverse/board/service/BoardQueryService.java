package cluverse.board.service;

import cluverse.board.service.implement.BoardReader;
import cluverse.board.service.request.BoardSearchRequest;
import cluverse.board.service.response.BoardDetailResponse;
import cluverse.board.service.response.BoardDirectoryResponse;
import cluverse.board.service.response.BoardHomeResponse;
import cluverse.member.service.implement.MemberReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardQueryService {

    private final BoardReader boardReader;
    private final MemberReader memberReader;

    public BoardDirectoryResponse getBoardDirectory(Long memberId, BoardSearchRequest request) {
        return boardReader.readBoardDirectory(memberId, memberReader.isVerified(memberId), request);
    }

    public BoardDetailResponse getBoard(Long memberId, Long boardId) {
        return boardReader.readBoardDetail(memberId, memberReader.isVerified(memberId), boardId);
    }

    public BoardHomeResponse getBoardHome(Long memberId, Long boardId) {
        return boardReader.readBoardHome(memberId, memberReader.isVerified(memberId), boardId);
    }

    public void validateReadableBoard(Long memberId, Long boardId) {
        boardReader.validateReadable(memberId, boardId);
    }

    public void validateWritableBoard(Long memberId, Long boardId) {
        boardReader.validateWritable(memberId, memberReader.isVerified(memberId), boardId);
    }
}
