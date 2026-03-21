package cluverse.post.service;

import cluverse.board.service.implement.BoardReader;
import cluverse.common.exception.ForbiddenException;
import cluverse.member.service.implement.MemberReader;
import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.post.domain.Post;
import cluverse.post.exception.PostExceptionMessage;
import cluverse.post.service.implement.PostAccessReader;
import cluverse.post.service.implement.PostWriter;
import cluverse.post.service.request.PostCreateRequest;
import cluverse.post.service.request.PostUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostAccessReader postAccessReader;
    private final PostWriter postWriter;
    private final BoardReader boardReader;
    private final MemberReader memberReader;
    private final PostMetaWriter postMetaWriter;

    public Long createPost(Long memberId, PostCreateRequest request, String clientIp) {
        boardReader.validateWritable(memberId, memberReader.isVerified(memberId), request.boardId());
        Post post = postWriter.create(memberId, request, clientIp);
        postMetaWriter.createViewCount(post.getId());
        return post.getId();
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
        Post post = postAccessReader.readOrThrow(postId);
        validateAuthor(memberId, post);
        postWriter.update(post, request);
        return postId;
    }

    public void deletePost(Long memberId, Long postId) {
        Post post = postAccessReader.readOrThrow(postId);
        validateAuthor(memberId, post);
        postWriter.delete(post);
    }

    private void validateAuthor(Long memberId, Post post) {
        if (!post.isAuthor(memberId)) {
            throw new ForbiddenException(PostExceptionMessage.POST_ACCESS_DENIED.getMessage());
        }
    }
}
