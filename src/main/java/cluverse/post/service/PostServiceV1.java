package cluverse.post.service;

import cluverse.board.service.implement.BoardReader;
import cluverse.comment.service.response.CommentLastRepliedPost;
import cluverse.comment.service.implement.CommentReader;
import cluverse.common.exception.ForbiddenException;
import cluverse.member.service.implement.MemberReader;
import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.post.domain.Post;
import cluverse.post.exception.PostExceptionMessage;
import cluverse.post.repository.PostQueryRepository;
import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.service.implement.PostAccessReader;
import cluverse.post.service.implement.PostWriter;
import cluverse.post.service.request.PostCreateRequest;
import cluverse.post.service.request.PostKeywordSearchRequest;
import cluverse.post.service.request.PostSearchRequest;
import cluverse.post.service.request.PostUpdateRequest;
import cluverse.post.service.response.PostDetailResponse;
import cluverse.post.service.response.PostPageResponse;
import cluverse.post.service.response.PostSummaryResponse;
import cluverse.post.service.response.PostTitleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@Service
@RequiredArgsConstructor
@Transactional
public class PostServiceV1 implements PostService {

    private final PostAccessReader postAccessReader;
    private final PostWriter postWriter;
    private final PostQueryRepository postQueryRepository;
    private final BoardReader boardReader;
    private final MemberReader memberReader;
    private final PostMetaWriter postMetaWriter;
    private final CommentReader commentReader;

    @Override
    @Transactional(readOnly = true)
    public PostPageResponse getPosts(Long memberId, PostSearchRequest request) {
        boardReader.validateReadable(memberId, request.boardId());

        PostPageQueryResult queryResult = request.isDateBased()
                ? postQueryRepository.findPostPageByDate(memberId, request)
                : postQueryRepository.findPostPage(memberId, request);

        List<PostSummaryResponse> responses = queryResult.posts().stream()
                .map(PostSummaryResponse::from)
                .toList();

        return new PostPageResponse(
                responses,
                request.isDateBased() ? null : request.pageOrDefault(),
                request.sizeOrDefault(),
                queryResult.hasNext(),
                request.isDateBased()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PostPageResponse searchPosts(Long memberId, PostKeywordSearchRequest request) {
        boardReader.validateReadable(memberId, request.boardId());

        PostPageQueryResult queryResult = postQueryRepository.findPostPageByKeyword(memberId, request);
        List<PostSummaryResponse> responses = queryResult.posts().stream()
                .map(PostSummaryResponse::from)
                .toList();

        return new PostPageResponse(
                responses,
                request.pageOrDefault(),
                request.sizeOrDefault(),
                queryResult.hasNext(),
                false
        );
    }

    @Override
    public PostDetailResponse createPost(Long memberId, PostCreateRequest request, String clientIp) {
        boardReader.validateWritable(memberId, memberReader.isVerified(memberId), request.boardId());
        Post post = postWriter.create(memberId, request, clientIp);
        postMetaWriter.createViewCount(post.getId());
        return PostDetailResponse.from(postQueryRepository.findPostDetail(memberId, post.getId()));
    }

    @Override
    public PostDetailResponse readPost(Long memberId, Long postId) {
        Post post = postAccessReader.readOrThrow(postId);
        boardReader.validateReadable(memberId, post.getBoardId());
        postMetaWriter.increaseViewCount(postId);
        return PostDetailResponse.from(postQueryRepository.findPostDetail(memberId, postId));
    }

    @Override
    public void increaseViewCount(Long postId) {
        postAccessReader.readOrThrow(postId);
        postMetaWriter.increaseViewCount(postId);
    }

    @Override
    public PostDetailResponse updatePost(Long memberId, Long postId, PostUpdateRequest request) {
        Post post = postAccessReader.readOrThrow(postId);
        validateAuthor(memberId, post);
        postWriter.update(post, request);
        return PostDetailResponse.from(postQueryRepository.findPostDetail(memberId, postId));
    }

    @Override
    public void deletePost(Long memberId, Long postId) {
        Post post = postAccessReader.readOrThrow(postId);
        validateAuthor(memberId, post);
        postWriter.delete(post);
    }

    @Override
    public void validatePostExists(Long postId) {
        postAccessReader.validatePostExists(postId);
    }

    @Override
    public void validateReadablePost(Long memberId, Long postId) {
        postAccessReader.validateReadablePost(memberId, postId);
    }

    @Override
    public void validateWritablePost(Long memberId, Long postId) {
        postAccessReader.validateWritablePost(memberId, postId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostTitleResponse> getRecentCommentRepliedPosts(Long size) {
        List<CommentLastRepliedPost> commentLastRepliedPosts = commentReader.readRecentCommentRepliedPosts(size);
        if (commentLastRepliedPosts.isEmpty()) {
            return List.of();
        }

        List<Long> postIds = commentLastRepliedPosts.stream()
                .map(CommentLastRepliedPost::postId)
                .toList();
        Map<Long, Post> postMap = postAccessReader.readPosts(postIds).stream()
                .filter(Post::isActive)
                .collect(toMap(Post::getId, Function.identity()));

        return commentLastRepliedPosts.stream()
                .map(commentLastRepliedPost -> {
                    Post post = postMap.get(commentLastRepliedPost.postId());
                    if (post == null) {
                        return null;
                    }
                    return new PostTitleResponse(
                            post.getId(),
                            post.getTitle(),
                            commentLastRepliedPost.lastCommentRepliedAt()
                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private void validateAuthor(Long memberId, Post post) {
        if (!post.isAuthor(memberId)) {
            throw new ForbiddenException(PostExceptionMessage.POST_ACCESS_DENIED.getMessage());
        }
    }
}
