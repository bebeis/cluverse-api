package cluverse.post.service;

import cluverse.board.service.BoardService;
import cluverse.post.domain.Post;
import cluverse.post.service.implement.PostReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostAccessServiceV1 implements PostAccessService {

    private final PostReader postReader;
    private final BoardService boardService;

    @Override
    public void validatePostExists(Long postId) {
        postReader.readOrThrow(postId);
    }

    @Override
    public void validateReadablePost(Long memberId, Long postId) {
        Post post = postReader.readOrThrow(postId);
        boardService.validateReadableBoard(memberId, post.getBoardId());
    }

    @Override
    public void validateWritablePost(Long memberId, Long postId) {
        Post post = postReader.readOrThrow(postId);
        boardService.validateWritableBoard(memberId, post.getBoardId());
    }
}
