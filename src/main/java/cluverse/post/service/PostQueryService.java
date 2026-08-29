package cluverse.post.service;

import cluverse.board.service.implement.BoardReader;
import cluverse.comment.service.implement.CommentReader;
import cluverse.comment.service.response.CommentLastRepliedPost;
import cluverse.member.service.implement.MemberReader;
import cluverse.post.domain.Post;
import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.repository.dto.PostSummaryQueryDto;
import cluverse.post.service.implement.PostAccessReader;
import cluverse.post.service.implement.PostListReader;
import cluverse.post.service.implement.PostReader;
import cluverse.post.service.request.PostKeywordSearchRequest;
import cluverse.post.service.request.PostPageSearchRequest;
import cluverse.post.service.request.PostSortType;
import cluverse.post.service.response.PostCursorResponse;
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

import static java.util.stream.Collectors.toMap;

@Service
@RequiredArgsConstructor
public class PostQueryService {

    private static final int PAGE_BLOCK_SIZE = 10;
    private static final int MAX_OFFSET_PAGE = 200;

    private final PostAccessReader postAccessReader;
    private final PostReader postReader;
    private final PostListReader postListReader;
    private final BoardReader boardReader;
    private final MemberReader memberReader;
    private final CommentReader commentReader;

    public PostPageResponse getPosts(Long memberId, PostPageSearchRequest request) {
        boardReader.validateReadable(memberId, request.boardId());

        int page = request.pageOrDefault();
        int size = request.sizeOrDefault();
        long searchLimit = pageBlockSearchLimit(page, size);
        PostPageQueryResult queryResult = postListReader.readPostPage(memberId, request, searchLimit);
        List<PostSummaryResponse> responses = queryResult.posts().stream()
                .map(PostSummaryResponse::from)
                .toList();

        long cappedCount = queryResult.cappedCount() == null
                ? postReader.countPostsUpTo(request, searchLimit)
                : queryResult.cappedCount();
        PageBlock pageBlock = resolvePageBlock(page, size, cappedCount);
        boolean cursorAvailable = request.sortOrDefault() == PostSortType.LATEST;
        return new PostPageResponse(
                responses,
                page,
                size,
                queryResult.hasNext(),
                pageBlock.lastPage(),
                pageBlock.hasNextBlock(),
                false,
                page > 1,
                cursorAvailable ? toCursor(firstOf(queryResult.posts())) : null,
                cursorAvailable ? toCursor(lastOf(queryResult.posts())) : null,
                cursorAvailable && page == MAX_OFFSET_PAGE && queryResult.hasNext()
        );
    }

    public PostPageResponse searchPosts(Long memberId, PostKeywordSearchRequest request) {
        boardReader.validateReadable(memberId, request.boardId());

        PostPageQueryResult queryResult = postReader.readPostPageByKeyword(memberId, request);
        List<PostSummaryResponse> responses = queryResult.posts().stream()
                .map(PostSummaryResponse::from)
                .toList();

        int page = request.pageOrDefault();
        int size = request.sizeOrDefault();
        long searchLimit = pageBlockSearchLimit(page, size);
        PageBlock pageBlock = resolvePageBlock(
                page, size, postReader.countPostsByKeywordUpTo(request, searchLimit));
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
    private long pageBlockSearchLimit(int page, int size) {
        int blockIndex = (page - 1) / PAGE_BLOCK_SIZE;
        return (long) (blockIndex + 1) * size * PAGE_BLOCK_SIZE + 1;
    }

    private PageBlock resolvePageBlock(int page, int size, long cappedCount) {
        int blockIndex = (page - 1) / PAGE_BLOCK_SIZE;
        long searchLimit = pageBlockSearchLimit(page, size);

        if (cappedCount >= searchLimit) {
            return new PageBlock((blockIndex + 1) * PAGE_BLOCK_SIZE, true);
        }
        int lastPage = (int) Math.max(1, (cappedCount + size - 1) / size);
        return new PageBlock(lastPage, false);
    }

    private record PageBlock(int lastPage, boolean hasNextBlock) {
    }

    private PostCursorResponse toCursor(PostSummaryQueryDto post) {
        return post == null ? null : new PostCursorResponse(post.createdAt(), post.postId());
    }

    private PostSummaryQueryDto firstOf(List<PostSummaryQueryDto> posts) {
        return posts.isEmpty() ? null : posts.getFirst();
    }

    private PostSummaryQueryDto lastOf(List<PostSummaryQueryDto> posts) {
        return posts.isEmpty() ? null : posts.getLast();
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
