package cluverse.post.service.implement;

import cluverse.common.exception.NotFoundException;
import cluverse.post.domain.Post;
import cluverse.post.exception.PostExceptionMessage;
import cluverse.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostReader {

    private final PostRepository postRepository;

    public Post readOrThrow(Long postId) {
        Post post = postRepository.findWithImagesById(postId)
                .orElseThrow(() -> new NotFoundException(PostExceptionMessage.POST_NOT_FOUND.getMessage()));
        validateActive(post);
        return post;
    }

    @Transactional
    public Post readForUpdateOrThrow(Long postId) {
        Post post = postRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new NotFoundException(PostExceptionMessage.POST_NOT_FOUND.getMessage()));
        validateActive(post);
        return post;
    }

    private void validateActive(Post post) {
        if (!post.isActive()) {
            throw new NotFoundException(PostExceptionMessage.POST_NOT_FOUND.getMessage());
        }
    }
}
