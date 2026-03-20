package cluverse.post.service;

import cluverse.board.service.BoardService;
import cluverse.common.exception.ForbiddenException;
import cluverse.meta.service.PostMetaService;
import cluverse.post.domain.Post;
import cluverse.post.exception.PostExceptionMessage;
import cluverse.post.repository.PostQueryRepository;
import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.service.implement.PostReader;
import cluverse.post.service.implement.PostWriter;
import cluverse.post.service.request.PostCreateRequest;
import cluverse.post.service.request.PostKeywordSearchRequest;
import cluverse.post.service.request.PostSearchRequest;
import cluverse.post.service.request.PostUpdateRequest;
import cluverse.post.service.response.PostDetailResponse;
import cluverse.post.service.response.PostPageResponse;
import cluverse.post.service.response.PostSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PostServiceV1 implements PostService {

    private final PostReader postReader;
    private final PostWriter postWriter;
    private final PostQueryRepository postQueryRepository;
    private final BoardService boardService;
    private final PostMetaService postMetaService;

    @Override
    @Transactional(readOnly = true)
    public PostPageResponse getPosts(Long memberId, PostSearchRequest request) {
        boardService.validateReadableBoard(memberId, request.boardId());

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
        boardService.validateReadableBoard(memberId, request.boardId());

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
        boardService.validateWritableBoard(memberId, request.boardId());
        Post post = postWriter.create(memberId, request, clientIp);
        postMetaService.createViewCount(post.getId());
        return PostDetailResponse.from(postQueryRepository.findPostDetail(memberId, post.getId()));
    }

    @Override
    public PostDetailResponse readPost(Long memberId, Long postId) {
        Post post = postReader.readOrThrow(postId);
        boardService.validateReadableBoard(memberId, post.getBoardId());
        postMetaService.increaseViewCount(postId);
        return PostDetailResponse.from(postQueryRepository.findPostDetail(memberId, postId));
    }

    @Override
    public void increaseViewCount(Long postId) {
        postReader.readOrThrow(postId);
        postMetaService.increaseViewCount(postId);
    }

    @Override
    public PostDetailResponse updatePost(Long memberId, Long postId, PostUpdateRequest request) {
        Post post = postReader.readOrThrow(postId);
        validateAuthor(memberId, post);
        postWriter.update(post, request);
        return PostDetailResponse.from(postQueryRepository.findPostDetail(memberId, postId));
    }

    @Override
    public void deletePost(Long memberId, Long postId) {
        Post post = postReader.readOrThrow(postId);
        validateAuthor(memberId, post);
        postWriter.delete(post);
    }

    @Override
    public void validatePostExists(Long postId) {
        postReader.readOrThrow(postId);
    }

    @Override
    public void validateReadablePost(Long memberId, Long postId) {
        Post post = postReader.readOrThrow(postId);
        boardService.validateReadableBoard(memberId, post.getBoardId());
    }

    @Override
    public void validateWritablePost(Long memberId, Long postId) {
        Post post = postReader.readOrThrow(postId);
        boardService.validateWritableBoard(memberId, post.getBoardId());
    }

    private void validateAuthor(Long memberId, Post post) {
        if (!post.isAuthor(memberId)) {
            throw new ForbiddenException(PostExceptionMessage.POST_ACCESS_DENIED.getMessage());
        }
    }
}
