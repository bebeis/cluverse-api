package cluverse.post.service;

import cluverse.board.service.implement.BoardReader;
import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.post.domain.Post;
import cluverse.post.service.implement.PostAccessReader;
import cluverse.post.service.implement.PostCreationProcessor;
import cluverse.post.service.implement.PostWriter;
import cluverse.post.service.request.PostCreateRequest;
import cluverse.post.service.request.PostUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostAccessReader postAccessReader;
    private final PostWriter postWriter;
    private final BoardReader boardReader;
    private final PostMetaWriter postMetaWriter;
    private final PostCreationProcessor postCreationProcessor;

    public Long createPost(Long memberId, PostCreateRequest request, String clientIp) {
        return postCreationProcessor.create(memberId, request, clientIp);
    }

    public void increaseViewCount(Long memberId, Long postId) {
        Post post = postAccessReader.readOrThrow(postId);
        boardReader.validateReadable(memberId, post.getBoardId());
        postMetaWriter.increaseViewCount(postId);
    }

    public void increaseViewCount(Long postId) {
        postAccessReader.readOrThrow(postId);
        postMetaWriter.increaseViewCount(postId);
    }

    public Long updatePost(Long memberId, Long postId, PostUpdateRequest request) {
        postWriter.update(memberId, postId, request);
        return postId;
    }

    public void deletePost(Long memberId, Long postId) {
        postWriter.delete(memberId, postId);
    }
}
