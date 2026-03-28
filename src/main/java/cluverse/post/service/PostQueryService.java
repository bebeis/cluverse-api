package cluverse.post.service;

import cluverse.board.service.implement.BoardReader;
import cluverse.comment.service.implement.CommentReader;
import cluverse.comment.service.response.CommentLastRepliedPost;
import cluverse.member.service.implement.MemberReader;
import cluverse.post.domain.Post;
import cluverse.post.repository.PostQueryRepository;
import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.service.implement.PostAccessReader;
import cluverse.post.service.request.PostKeywordSearchRequest;
import cluverse.post.service.request.PostSearchRequest;
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
@Transactional(readOnly = true)
public class PostQueryService {

    private final PostAccessReader postAccessReader;
    private final PostQueryRepository postQueryRepository;
    private final BoardReader boardReader;
    private final MemberReader memberReader;
    private final CommentReader commentReader;

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

    public PostDetailResponse readPost(Long memberId, Long postId) {
        Post post = postAccessReader.readOrThrow(postId);
        boardReader.validateReadable(memberId, post.getBoardId());
        return PostDetailResponse.from(postQueryRepository.findPostDetail(memberId, postId));
    }

    public void validatePostExists(Long postId) {
        postAccessReader.validatePostExists(postId);
    }

    public void validateReadablePost(Long memberId, Long postId) {
        postAccessReader.validateReadablePost(memberId, postId);
    }

    public void validateWritablePost(Long memberId, Long postId) {
        postAccessReader.validateWritablePost(memberId, postId);
    }

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
}
