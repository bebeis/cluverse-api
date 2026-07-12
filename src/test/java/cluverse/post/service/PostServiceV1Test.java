package cluverse.post.service;

import cluverse.board.service.implement.BoardReader;
import cluverse.board.domain.BoardType;
import cluverse.comment.service.implement.CommentReader;
import cluverse.comment.service.response.CommentLastRepliedPost;
import cluverse.member.service.implement.MemberReader;
import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.post.domain.Post;
import cluverse.post.domain.PostCategory;
import cluverse.post.service.implement.PostReader;
import cluverse.post.repository.dto.PostDetailQueryDto;
import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.repository.dto.PostSummaryQueryDto;
import cluverse.post.service.implement.PostAccessReader;
import cluverse.post.service.implement.PostCreationProcessor;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceV1Test {

    @Mock
    private PostAccessReader postAccessReader;

    @Mock
    private PostWriter postWriter;

    @Mock
    private PostReader postReader;

    @Mock
    private BoardReader boardReader;

    @Mock
    private MemberReader memberReader;

    @Mock
    private PostMetaWriter postMetaWriter;

    @Mock
    private CommentReader commentReader;

    @Mock
    private PostCreationProcessor postCreationProcessor;

    @InjectMocks
    private PostQueryService postQueryService;

    @InjectMocks
    private PostService postService;

    @Test
    void 게시글_목록_조회시_서비스가_정렬된_ID_순서대로_응답을_조립한다() {
        // given
        PostSearchRequest request = new PostSearchRequest(3L, null, PostSortType.LATEST, 1, 20, null);
        when(postReader.readPostPage(99L, request)).thenReturn(new PostPageQueryResult(
                List.of(
                        createPostSummaryQueryDto(2L, 20L, false),
                        createPostSummaryQueryDto(1L, 10L, false)
                ),
                true
        ));
        when(postReader.countPostsUpTo(request, 201L)).thenReturn(201L);

        // when
        PostPageResponse response = postQueryService.getPosts(99L, request);

        // then
        assertThat(response.posts()).extracting("postId").containsExactly(2L, 1L);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.lastPage()).isEqualTo(10);
        assertThat(response.hasNextBlock()).isTrue();
        assertThat(response.dateBased()).isFalse();
        verify(boardReader).validateReadable(99L, 3L);
    }

    @Test
    void 게시글_수가_카운트_상한_미만이면_실제_마지막_페이지를_계산한다() {
        // given
        PostSearchRequest request = new PostSearchRequest(3L, null, PostSortType.LATEST, 1, 20, null);
        when(postReader.readPostPage(99L, request)).thenReturn(new PostPageQueryResult(
                List.of(createPostSummaryQueryDto(2L, 20L, false)),
                false
        ));
        when(postReader.countPostsUpTo(request, 201L)).thenReturn(35L);

        // when
        PostPageResponse response = postQueryService.getPosts(99L, request);

        // then
        assertThat(response.lastPage()).isEqualTo(2);
        assertThat(response.hasNextBlock()).isFalse();
    }

    @Test
    void 날짜_기반_목록_조회시_date_기반_쿼리를_호출하고_page는_null이다() {
        // given
        LocalDate date = LocalDate.of(2024, 1, 15);
        PostSearchRequest request = new PostSearchRequest(3L, null, null, null, 20, date);
        when(postReader.readPostPageByDate(99L, request)).thenReturn(new PostPageQueryResult(
                List.of(
                        createPostSummaryQueryDto(5L, 20L, false),
                        createPostSummaryQueryDto(4L, 20L, false)
                ),
                false
        ));

        // when
        PostPageResponse response = postQueryService.getPosts(99L, request);

        // then
        assertThat(response.posts()).extracting("postId").containsExactly(5L, 4L);
        assertThat(response.page()).isNull();
        assertThat(response.lastPage()).isNull();
        assertThat(response.hasNextBlock()).isNull();
        assertThat(response.dateBased()).isTrue();
        assertThat(response.hasNext()).isFalse();
        verify(boardReader).validateReadable(99L, 3L);
    }

    @Test
    void 게시글_검색시_검색_결과를_응답으로_조립한다() {
        // given
        PostKeywordSearchRequest request = new PostKeywordSearchRequest(3L, "스프링", 1, 20);
        when(postReader.readPostPageByKeyword(99L, request)).thenReturn(new PostPageQueryResult(
                List.of(createPostSummaryQueryDto(10L, 20L, false)),
                true
        ));
        when(postReader.countPostsByKeywordUpTo(request, 201L)).thenReturn(35L);

        // when
        PostPageResponse response = postQueryService.searchPosts(99L, request);

        // then
        assertThat(response.posts()).extracting("postId").containsExactly(10L);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.lastPage()).isEqualTo(2);
        assertThat(response.hasNextBlock()).isFalse();
        assertThat(response.hasNext()).isTrue();
        assertThat(response.dateBased()).isFalse();
        verify(boardReader).validateReadable(99L, 3L);
    }

    @Test
    void 익명_게시글_상세_조회시_작성자_정보를_가린다() {
        // given
        Post post = createPost(10L, 3L, 1L, "익명 질문", true);
        when(postAccessReader.readOrThrow(10L)).thenReturn(post);
        when(postReader.readPostDetail(2L, 10L)).thenReturn(createAnonymousPostDetailQueryDto());

        // when
        PostDetailResponse response = postQueryService.readPost(2L, 10L);

        // then
        assertThat(response.isAnonymous()).isTrue();
        assertThat(response.author().memberId()).isNull();
        assertThat(response.author().nickname()).isEqualTo("익명");
        verify(boardReader).validateReadable(2L, 3L);
    }

    @Test
    void 게시글_조회수_증가는_meta_서비스에게_위임한다() {
        // given
        Post post = createPost(10L, 3L, 1L, "조회수 증가 대상", false);
        when(postAccessReader.readOrThrow(10L)).thenReturn(post);

        // when
        postService.increaseViewCount(10L);

        // then
        verify(postAccessReader).readOrThrow(10L);
        verify(postMetaWriter).increaseViewCount(10L);
    }

    @Test
    void 게시글_수정은_Writer에_위임하고_게시글ID를_반환한다() {
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

        Long result = postService.updatePost(1L, 10L, request);

        assertThat(result).isEqualTo(10L);
        verify(postWriter).update(1L, 10L, request);
    }

    @Test
    void 게시글_삭제는_Writer에_위임한다() {
        postService.deletePost(1L, 10L);

        verify(postWriter).delete(1L, 10L);
    }

    @Test
    void 게시글_존재_검증은_리더에게_위임한다() {
        // when
        postQueryService.validatePostExists(10L);

        // then
        verify(postAccessReader).validatePostExists(10L);
    }

    @Test
    void 게시글_작성은_Processor에_위임하고_생성된_ID를_반환한다() {
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
        when(postCreationProcessor.create(1L, request, "127.0.0.1")).thenReturn(10L);

        // when
        Long postId = postService.createPost(1L, request, "127.0.0.1");

        // then
        assertThat(postId).isEqualTo(10L);
        verify(postCreationProcessor).create(1L, request, "127.0.0.1");
    }

    @Test
    void 게시글_읽기_권한_검증은_게시판_서비스를_통해_수행한다() {
        // when
        postQueryService.validateReadablePost(7L, 10L);

        // then
        verify(postAccessReader).validateReadablePost(7L, 10L);
    }

    @Test
    void 최근_댓글이_달린_게시글은_최근_댓글_시각_순서대로_응답한다() {
        // given
        LocalDateTime latest = LocalDateTime.of(2026, 3, 20, 12, 0);
        LocalDateTime previous = LocalDateTime.of(2026, 3, 20, 11, 0);
        Post firstPost = createPost(1L, 3L, 10L, "첫 번째 글", false);
        Post secondPost = createPost(2L, 3L, 20L, "두 번째 글", false);

        when(commentReader.readRecentCommentRepliedPosts(2L)).thenReturn(List.of(
                new CommentLastRepliedPost(2L, latest),
                new CommentLastRepliedPost(1L, previous)
        ));
        when(postAccessReader.readPosts(List.of(2L, 1L))).thenReturn(List.of(firstPost, secondPost));

        // when
        List<PostTitleResponse> response = postQueryService.getRecentCommentRepliedPosts(2L);

        // then
        assertThat(response).extracting(PostTitleResponse::postId).containsExactly(2L, 1L);
        assertThat(response).extracting(PostTitleResponse::title).containsExactly("두 번째 글", "첫 번째 글");
        assertThat(response).extracting(PostTitleResponse::lastCommentRepliedAt).containsExactly(latest, previous);
        verify(commentReader).readRecentCommentRepliedPosts(2L);
        verify(postAccessReader).readPosts(List.of(2L, 1L));
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
