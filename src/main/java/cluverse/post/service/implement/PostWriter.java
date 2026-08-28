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
    private final PostImageUploadClaimer imageUploadClaimer;
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
        imageUploadClaimer.claimForCreate(
                memberId, saved, request.imageUrls(), request.imageUploadRequestIds());
        eventPublisher.publishEvent(PostCreatedEvent.from(saved));
        return saved;
    }

    public Post create(Long memberId, PostCreateRequest request, String clientIp, String clientRequestId) {
        Post post = Post.createByMember(
                request.tags(), request.imageUrls(), request.boardId(), memberId, request.title(), request.content(),
                request.category(), request.isAnonymous(), request.isPinned(), request.isExternalVisible(), clientIp,
                clientRequestId
        );
        Post saved = postRepository.saveAndFlush(post);
        imageUploadClaimer.claimForCreate(
                memberId, saved, request.imageUrls(), request.imageUploadRequestIds());
        eventPublisher.publishEvent(PostCreatedEvent.from(saved));
        return saved;
    }

    public void update(Long memberId, Long postId, PostUpdateRequest request) {
        Post post = postAccessReader.readOrThrow(postId);
        validateAuthor(memberId, post);
        post.updateDetails(
                request.title(),
                request.content(),
                request.category(),
                request.tags(),
                request.isAnonymous(),
                request.isPinned(),
                request.isExternalVisible()
        );
        imageUploadClaimer.claimForUpdate(
                memberId,
                post,
                request.imageUrls(),
                request.retainedImageContentKeys(),
                request.imageUploadRequestIds()
        );
        eventPublisher.publishEvent(new PostListChangedEvent(post.getBoardId()));
    }

    public void delete(Long memberId, Long postId) {
        Post post = postAccessReader.readOrThrow(postId);
        validateAuthor(memberId, post);
        imageUploadClaimer.releaseAll(post);
        post.delete();
        eventPublisher.publishEvent(new PostListChangedEvent(post.getBoardId()));
    }

    private void validateAuthor(Long memberId, Post post) {
        if (!post.isAuthor(memberId)) {
            throw new ForbiddenException(PostExceptionMessage.POST_ACCESS_DENIED.getMessage());
        }
    }
}
