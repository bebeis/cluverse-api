package cluverse.post.service.implement;

import cluverse.common.exception.NotFoundException;
import cluverse.post.domain.Post;
import cluverse.post.exception.PostExceptionMessage;
import cluverse.post.repository.PostRepository;
import cluverse.post.service.request.PostCreateRequest;
import cluverse.post.service.request.PostUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class PostWriter {

    private final PostRepository postRepository;

    public Post create(Long memberId, PostCreateRequest request, String clientIp) {
        Post post = Post.createByMember(
                request.tags(),
                request.imageUrls(),
                request.boardId(),
                memberId,
                request.title(),
                request.content(),
                request.category(),
                request.isAnonymous(),
                request.isPinned(),
                request.isExternalVisible(),
                clientIp
        );
        return postRepository.save(post);
    }

    public void update(Post post, PostUpdateRequest request) {
        post.update(
                request.title(),
                request.content(),
                request.category(),
                request.tags(),
                request.imageUrls(),
                request.isAnonymous(),
                request.isPinned(),
                request.isExternalVisible()
        );
    }

    public void delete(Post post) {
        post.delete();
    }

    public void increaseViewCount(Long postId) {
        int updatedRowCount = postRepository.increaseViewCount(postId);
        validateUpdated(updatedRowCount);
    }

    public void increaseLikeCount(Post post) {
        post.increaseLikeCount();
    }

    public void decreaseLikeCount(Post post) {
        post.decreaseLikeCount();
    }

    public void increaseBookmarkCount(Post post) {
        post.increaseBookmarkCount();
    }

    public void decreaseBookmarkCount(Post post) {
        post.decreaseBookmarkCount();
    }

    private void validateUpdated(int updatedRowCount) {
        if (updatedRowCount == 0) {
            throw new NotFoundException(PostExceptionMessage.POST_NOT_FOUND.getMessage());
        }
    }
}
