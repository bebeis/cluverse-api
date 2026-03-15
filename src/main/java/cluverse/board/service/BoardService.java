package cluverse.board.service;

import cluverse.board.service.request.BoardSearchRequest;
import cluverse.board.service.response.BoardDetailResponse;
import cluverse.board.service.response.BoardDirectoryResponse;
import cluverse.board.service.response.BoardHomeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BoardService {

    private static final String NOT_IMPLEMENTED_MESSAGE =
            "Board API contract is defined, but the read model implementation is not completed yet.";

    public BoardDirectoryResponse getBoardDirectory(Long memberId, BoardSearchRequest request) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
    }

    public BoardDetailResponse getBoard(Long memberId, Long boardId) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
    }

    public BoardHomeResponse getBoardHome(Long memberId, Long boardId) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
    }
}
