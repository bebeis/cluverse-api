package cluverse.post.service;

import cluverse.board.service.BoardService;
import cluverse.board.domain.BoardType;
import cluverse.comment.service.CommentService;
import cluverse.comment.service.response.CommentLastRepliedPost;
import cluverse.common.exception.ForbiddenException;
import cluverse.meta.service.PostMetaService;
import cluverse.post.domain.Post;
import cluverse.post.domain.PostCategory;
import cluverse.post.repository.PostQueryRepository;
import cluverse.post.repository.dto.PostDetailQueryDto;
import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.repository.dto.PostSummaryQueryDto;
import cluverse.post.service.implement.PostReader;
import cluverse.post.service.implement.PostWriter;
import cluverse.post.service.request.PostCreateRequest;
import cluverse.post.service.request.PostKeywordSearchRequest;
import cluverse.post.service.request.PostSearchRequest;
import cluverse.post.service.request.PostSortType;
import cluverse.post.service.request.PostUpdateRequest;
import cluverse.post.service.response.PostAuthorResponse;
import cluverse.post.service.response.PostDetailResponse;
import cluverse.post.service.response.PostPageResponse;
import cluverse.post.service.response.PostTitleResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceV1Test {

    @Mock
    private PostReader postReader;

    @Mock
    private PostWriter postWriter;

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private BoardService boardService;

    @Mock
    private PostMetaService postMetaService;

    @Mock
    private CommentService commentService;

    @Mock
    private PostAccessService postAccessService;

    @InjectMocks
    private PostServiceV1 postService;

    @Test
    void 게시글_목록_조회시_서비스가_정렬된_ID_순서대로_응답을_조립한다() {
        // given
        PostSearchRequest request = new PostSearchRequest(3L, null, PostSortType.LATEST, 1, 20, null);
        when(postQueryRepository.findPostPage(99L, request)).thenReturn(new PostPageQueryResult(
                List.of(
                        createPostSummaryQueryDto(2L, 20L, false),
                        createPostSummaryQueryDto(1L, 10L, false)
                ),
                true
        ));

        // when
        PostPageResponse response = postService.getPosts(99L, request);

        // then
        assertThat(response.posts()).extracting("postId").containsExactly(2L, 1L);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.dateBased()).isFalse();
        verify(boardService).validateReadableBoard(99L, 3L);
    }

    @Test
    void 날짜_기반_목록_조회시_date_기반_쿼리를_호출하고_page는_null이다() {
        // given
        LocalDate date = LocalDate.of(2024, 1, 15);
        PostSearchRequest request = new PostSearchRequest(3L, null, null, null, 20, date);
        when(postQueryRepository.findPostPageByDate(99L, request)).thenReturn(new PostPageQueryResult(
                List.of(
                        createPostSummaryQueryDto(5L, 20L, false),
                        createPostSummaryQueryDto(4L, 20L, false)
                ),
                false
        ));

        // when
        PostPageResponse response = postService.getPosts(99L, request);

        // then
        assertThat(response.posts()).extracting("postId").containsExactly(5L, 4L);
        assertThat(response.page()).isNull();
        assertThat(response.dateBased()).isTrue();
        assertThat(response.hasNext()).isFalse();
        verify(boardService).validateReadableBoard(99L, 3L);
    }

    @Test
    void 게시글_검색시_검색_결과를_응답으로_조립한다() {
        // given
        PostKeywordSearchRequest request = new PostKeywordSearchRequest(3L, "스프링", 1, 20);
        when(postQueryRepository.findPostPageByKeyword(99L, request)).thenReturn(new PostPageQueryResult(
                List.of(createPostSummaryQueryDto(10L, 20L, false)),
                true
        ));

        // when
        PostPageResponse response = postService.searchPosts(99L, request);

        // then
        assertThat(response.posts()).extracting("postId").containsExactly(10L);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.dateBased()).isFalse();
        verify(boardService).validateReadableBoard(99L, 3L);
    }

    @Test
    void 익명_게시글_상세_조회시_작성자_정보를_가린다() {
        // given
        Post post = createPost(10L, 3L, 1L, "익명 질문", true);
        when(postReader.readOrThrow(10L)).thenReturn(post);
        when(postQueryRepository.findPostDetail(2L, 10L)).thenReturn(createAnonymousPostDetailQueryDto());

        // when
        PostDetailResponse response = postService.readPost(2L, 10L);

        // then
        assertThat(response.isAnonymous()).isTrue();
        assertThat(response.author().memberId()).isNull();
        assertThat(response.author().nickname()).isEqualTo("익명");
        verify(boardService).validateReadableBoard(2L, 3L);
        verify(postMetaService).increaseViewCount(10L);
    }

    @Test
    void 게시글_조회수_증가는_meta_서비스에게_위임한다() {
        // given
        Post post = createPost(10L, 3L, 1L, "조회수 증가 대상", false);
        when(postReader.readOrThrow(10L)).thenReturn(post);

        // when
        postService.increaseViewCount(10L);

        // then
        verify(postReader).readOrThrow(10L);
        verify(postMetaService).increaseViewCount(10L);
    }

    @Test
    void 작성자가_아니면_게시글을_수정할_수_없다() {
        Post post = createPost(10L, 3L, 1L, "원본 글", false);
        PostUpdateRequest request = new PostUpdateRequest(
                "수정 제목",
                "수정 본문",
                PostCategory.INFORMATION,
                List.of("spring"),
                false,
                false,
                true,
                List.of()
        );

        when(postReader.readOrThrow(10L)).thenReturn(post);

        assertThatThrownBy(() -> postService.updatePost(2L, 10L, request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void 게시글_존재_검증은_리더에게_위임한다() {
        // when
        postService.validatePostExists(10L);

        // then
        verify(postAccessService).validatePostExists(10L);
    }

    @Test
    void 게시글_작성시_게시판_쓰기_권한을_검증한다() {
        // given
        PostCreateRequest request = new PostCreateRequest(
                3L,
                "제목",
                "본문",
                PostCategory.INFORMATION,
                List.of("spring"),
                false,
                false,
                true,
                List.of()
        );
        Post post = createPost(10L, 3L, 1L, "제목", false);
        when(postWriter.create(1L, request, "127.0.0.1")).thenReturn(post);
        when(postQueryRepository.findPostDetail(1L, 10L)).thenReturn(createAnonymousPostDetailQueryDto());

        // when
        postService.createPost(1L, request, "127.0.0.1");

        // then
        verify(boardService).validateWritableBoard(1L, 3L);
        verify(postMetaService).createViewCount(10L);
    }

    @Test
    void 게시글_읽기_권한_검증은_게시판_서비스를_통해_수행한다() {
        // when
        postService.validateReadablePost(7L, 10L);

        // then
        verify(postAccessService).validateReadablePost(7L, 10L);
    }

    @Test
    void 최근_댓글이_달린_게시글은_최근_댓글_시각_순서대로_응답한다() {
        // given
        LocalDateTime latest = LocalDateTime.of(2026, 3, 20, 12, 0);
        LocalDateTime previous = LocalDateTime.of(2026, 3, 20, 11, 0);
        Post firstPost = createPost(1L, 3L, 10L, "첫 번째 글", false);
        Post secondPost = createPost(2L, 3L, 20L, "두 번째 글", false);

        when(commentService.getRecentCommentRepliedPostIds(2L)).thenReturn(List.of(
                new CommentLastRepliedPost(2L, latest),
                new CommentLastRepliedPost(1L, previous)
        ));
        when(postReader.readPosts(List.of(2L, 1L))).thenReturn(List.of(firstPost, secondPost));

        // when
        List<PostTitleResponse> response = postService.getRecentCommentRepliedPosts(2L);

        // then
        assertThat(response).extracting(PostTitleResponse::postId).containsExactly(2L, 1L);
        assertThat(response).extracting(PostTitleResponse::title).containsExactly("두 번째 글", "첫 번째 글");
        assertThat(response).extracting(PostTitleResponse::lastCommentRepliedAt).containsExactly(latest, previous);
        verify(commentService).getRecentCommentRepliedPostIds(2L);
        verify(postReader).readPosts(List.of(2L, 1L));
    }

    private PostSummaryQueryDto createPostSummaryQueryDto(
            Long postId,
            Long authorMemberId,
            boolean isAnonymous
    ) {
        return new PostSummaryQueryDto(
                postId,
                3L,
                PostCategory.INFORMATION,
                "제목 %d".formatted(postId),
                "본문 미리보기입니다.",
                List.of("spring", "backend"),
                "https://cdn.example.com/posts/%d-thumb.png".formatted(postId),
                isAnonymous,
                false,
                true,
                false,
                120L,
                15L,
                4L,
                8L,
                authorMemberId,
                "writer-%d".formatted(authorMemberId),
                "https://cdn.example.com/%d.png".formatted(authorMemberId),
                LocalDateTime.of(2026, 3, 13, 14, 0)
        );
    }

    private PostDetailQueryDto createAnonymousPostDetailQueryDto() {
        return new PostDetailQueryDto(
                10L,
                3L,
                BoardType.INTEREST,
                "AI",
                null,
                PostCategory.INFORMATION,
                "익명 질문",
                "본문 내용입니다.",
                List.of("spring", "backend"),
                List.of("https://cdn.example.com/posts/10.png"),
                true,
                false,
                true,
                false,
                120L,
                15L,
                4L,
                8L,
                1L,
                "luna",
                "https://cdn.example.com/profile.png",
                LocalDateTime.of(2026, 3, 13, 14, 0),
                LocalDateTime.of(2026, 3, 13, 14, 0)
        );
    }

    private Post createPost(Long postId, Long boardId, Long memberId, String title, boolean isAnonymous) {
        Post post = Post.createByMember(
                List.of("spring", "backend"),
                List.of("https://cdn.example.com/posts/%d.png".formatted(postId)),
                boardId,
                memberId,
                title,
                "본문 내용입니다.",
                PostCategory.INFORMATION,
                isAnonymous,
                false,
                true,
                "127.0.0.1"
        );
        ReflectionTestUtils.setField(post, "id", postId);
        ReflectionTestUtils.setField(post, "createdAt", LocalDateTime.of(2026, 3, 13, 14, 0));
        ReflectionTestUtils.setField(post, "updatedAt", LocalDateTime.of(2026, 3, 13, 14, 0));
        return post;
    }
}
