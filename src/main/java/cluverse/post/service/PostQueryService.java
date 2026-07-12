package cluverse.post.service;

import cluverse.board.service.implement.BoardReader;
import cluverse.comment.service.implement.CommentReader;
import cluverse.comment.service.response.CommentLastRepliedPost;
import cluverse.member.service.implement.MemberReader;
import cluverse.post.domain.Post;
import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.service.implement.PostAccessReader;
import cluverse.post.service.implement.PostReader;
import cluverse.post.service.request.PostKeywordSearchRequest;
import cluverse.post.service.request.PostSearchRequest;
import cluverse.post.service.response.PostDetailResponse;
import cluverse.post.service.response.PostPageResponse;
import cluverse.post.service.response.PostSummaryResponse;
import cluverse.post.service.response.PostTitleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.LongUnaryOperator;

import static java.util.stream.Collectors.toMap;

@Service
@RequiredArgsConstructor
public class PostQueryService {

    private static final int PAGE_BLOCK_SIZE = 10;

    private final PostAccessReader postAccessReader;
    private final PostReader postReader;
    private final BoardReader boardReader;
    private final MemberReader memberReader;
    private final CommentReader commentReader;

    public PostPageResponse getPosts(Long memberId, PostSearchRequest request) {
        boardReader.validateReadable(memberId, request.boardId());

        PostPageQueryResult queryResult = request.isDateBased()
                ? postReader.readPostPageByDate(memberId, request)
                : postReader.readPostPage(memberId, request);

        List<PostSummaryResponse> responses = queryResult.posts().stream()
                .map(PostSummaryResponse::from)
                .toList();

        if (request.isDateBased()) {
            return new PostPageResponse(responses, null, request.sizeOrDefault(), queryResult.hasNext(), true);
        }

        PageBlock pageBlock = resolvePageBlock(
                request.pageOrDefault(),
                request.sizeOrDefault(),
                searchLimit -> postReader.countPostsUpTo(request, searchLimit)
        );
        return new PostPageResponse(
                responses,
                request.pageOrDefault(),
                request.sizeOrDefault(),
                queryResult.hasNext(),
                pageBlock.lastPage(),
                pageBlock.hasNextBlock(),
                false
        );
    }

    public PostPageResponse searchPosts(Long memberId, PostKeywordSearchRequest request) {
        boardReader.validateReadable(memberId, request.boardId());

        PostPageQueryResult queryResult = postReader.readPostPageByKeyword(memberId, request);
        List<PostSummaryResponse> responses = queryResult.posts().stream()
                .map(PostSummaryResponse::from)
                .toList();

        PageBlock pageBlock = resolvePageBlock(
                request.pageOrDefault(),
                request.sizeOrDefault(),
                searchLimit -> postReader.countPostsByKeywordUpTo(request, searchLimit)
        );
        return new PostPageResponse(
                responses,
                request.pageOrDefault(),
                request.sizeOrDefault(),
                queryResult.hasNext(),
                pageBlock.lastPage(),
                pageBlock.hasNextBlock(),
                false
        );
    }

    /**
     * 전체 게시글을 세지 않고, 현재 페이지 블록 렌더링에 필요한 상한
     * (((page - 1) / k) + 1) * size * k + 1 까지만 센다.
     * 상한에 도달하면 다음 블록이 존재한다는 뜻이므로 블록 끝 페이지를,
     * 미달이면 그 값이 정확한 전체 개수이므로 실제 마지막 페이지를 계산한다.
     */
    private PageBlock resolvePageBlock(int page, int size, LongUnaryOperator countUpTo) {
        int blockIndex = (page - 1) / PAGE_BLOCK_SIZE;
        long searchLimit = (long) (blockIndex + 1) * size * PAGE_BLOCK_SIZE + 1;
        long cappedCount = countUpTo.applyAsLong(searchLimit);

        if (cappedCount >= searchLimit) {
            return new PageBlock((blockIndex + 1) * PAGE_BLOCK_SIZE, true);
        }
        int lastPage = (int) Math.max(1, (cappedCount + size - 1) / size);
        return new PageBlock(lastPage, false);
    }

    private record PageBlock(int lastPage, boolean hasNextBlock) {
    }

    public PostDetailResponse readPost(Long memberId, Long postId) {
        Post post = postAccessReader.readOrThrow(postId);
        boardReader.validateReadable(memberId, post.getBoardId());
        return PostDetailResponse.from(postReader.readPostDetail(memberId, postId));
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
