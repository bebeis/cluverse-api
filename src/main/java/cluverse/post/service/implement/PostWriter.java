package cluverse.post.service.implement;

import cluverse.common.exception.ForbiddenException;
import cluverse.post.domain.Post;
import cluverse.post.exception.PostExceptionMessage;
import cluverse.post.repository.PostRepository;
import cluverse.post.service.request.PostCreateRequest;
import cluverse.post.service.request.PostUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class PostWriter {

    private final PostRepository postRepository;
    private final PostAccessReader postAccessReader;
    private final ApplicationEventPublisher eventPublisher;

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
        Post saved = postRepository.save(post);
        eventPublisher.publishEvent(new PostListChangedEvent(post.getBoardId()));
        return saved;
    }

    public Post create(Long memberId, PostCreateRequest request, String clientIp, String clientRequestId) {
        Post post = Post.createByMember(
                request.tags(), request.imageUrls(), request.boardId(), memberId, request.title(), request.content(),
                request.category(), request.isAnonymous(), request.isPinned(), request.isExternalVisible(), clientIp,
                clientRequestId
        );
        Post saved = postRepository.saveAndFlush(post);
        eventPublisher.publishEvent(new PostListChangedEvent(post.getBoardId()));
        return saved;
    }

    public void update(Long memberId, Long postId, PostUpdateRequest request) {
        Post post = postAccessReader.readOrThrow(postId);
        validateAuthor(memberId, post);
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
        eventPublisher.publishEvent(new PostListChangedEvent(post.getBoardId()));
    }

    public void delete(Long memberId, Long postId) {
        Post post = postAccessReader.readOrThrow(postId);
        validateAuthor(memberId, post);
        post.delete();
        eventPublisher.publishEvent(new PostListChangedEvent(post.getBoardId()));
    }

    private void validateAuthor(Long memberId, Post post) {
        if (!post.isAuthor(memberId)) {
            throw new ForbiddenException(PostExceptionMessage.POST_ACCESS_DENIED.getMessage());
        }
    }
}
