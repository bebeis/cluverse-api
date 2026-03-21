package cluverse.post.service.implement;

import cluverse.board.service.implement.BoardReader;
import cluverse.common.exception.NotFoundException;
import cluverse.member.service.implement.MemberReader;
import cluverse.post.domain.Post;
import cluverse.post.exception.PostExceptionMessage;
import cluverse.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostAccessReader {

    private final PostRepository postRepository;
    private final BoardReader boardReader;
    private final MemberReader memberReader;

    public Post readOrThrow(Long postId) {
        Post post = postRepository.findWithImagesById(postId)
                .orElseThrow(() -> new NotFoundException(PostExceptionMessage.POST_NOT_FOUND.getMessage()));
        validateActive(post);
        return post;
    }

    public List<Post> readPosts(List<Long> postIds) {
        return postRepository.findAllById(postIds);
    }

    public void validatePostExists(Long postId) {
        readOrThrow(postId);
    }

    public void validateReadablePost(Long memberId, Long postId) {
        Post post = readOrThrow(postId);
        boardReader.validateReadable(memberId, post.getBoardId());
    }

    public void validateWritablePost(Long memberId, Long postId) {
        Post post = readOrThrow(postId);
        boardReader.validateWritable(memberId, memberReader.isVerified(memberId), post.getBoardId());
    }

    private void validateActive(Post post) {
        if (!post.isActive()) {
            throw new NotFoundException(PostExceptionMessage.POST_NOT_FOUND.getMessage());
        }
    }
}
