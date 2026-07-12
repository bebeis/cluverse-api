package cluverse.post.service.implement;

import cluverse.board.domain.Board;
import cluverse.board.domain.BoardType;
import cluverse.board.repository.BoardRepository;
import cluverse.common.config.QuerydslConfig;
import cluverse.common.exception.NotFoundException;
import cluverse.member.domain.Member;
import cluverse.member.repository.MemberRepository;
import cluverse.meta.domain.PostViewCount;
import cluverse.meta.repository.PostViewCountRepository;
import cluverse.post.domain.Post;
import cluverse.post.domain.PostCategory;
import cluverse.post.repository.PostPageQueryRepository;
import cluverse.post.repository.PostQueryRepository;
import cluverse.post.repository.PostRepository;
import cluverse.post.repository.dto.PostDetailQueryDto;
import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.repository.dto.PostSummaryQueryDto;
import cluverse.post.service.request.PostKeywordSearchRequest;
import cluverse.post.service.request.PostSearchRequest;
import cluverse.post.service.request.PostSortType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({PostReader.class, PostPageQueryRepository.class, PostQueryRepository.class, QuerydslConfig.class})
class PostReaderTest {

    @Autowired
    private PostReader postReader;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private PostViewCountRepository postViewCountRepository;

    @Test
    void 최신순_목록_조회는_뷰카운트_조인_없이_ID_역순으로_페이징된다() {
        // given
        Member author = memberRepository.save(Member.createSocialMember("author"));
        Post post1 = postRepository.save(createPost(1L, author.getId(), "글1"));
        Post post2 = postRepository.save(createPost(1L, author.getId(), "글2"));
        Post post3 = postRepository.save(createPost(1L, author.getId(), "글3"));
        postRepository.save(createPost(2L, author.getId(), "다른 게시판"));

        PostSearchRequest request = new PostSearchRequest(1L, null, PostSortType.LATEST, 1, 2, null);

        // when
        PostPageQueryResult result = postReader.readPostPage(author.getId(), request);

        // then
        assertThat(result.posts()).extracting(PostSummaryQueryDto::postId)
                .containsExactly(post3.getId(), post2.getId());
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void 조회수순_정렬은_뷰카운트를_조인해서_정렬한다() {
        // given
        Member author = memberRepository.save(Member.createSocialMember("author"));
        Post post1 = postRepository.save(createPost(1L, author.getId(), "글1"));
        Post post2 = postRepository.save(createPost(1L, author.getId(), "글2"));
        Post post3 = postRepository.save(createPost(1L, author.getId(), "글3"));
        postViewCountRepository.save(PostViewCount.of(post1.getId(), 5));
        postViewCountRepository.save(PostViewCount.of(post3.getId(), 1));

        PostSearchRequest request = new PostSearchRequest(1L, null, PostSortType.VIEW_COUNT, 1, 10, null);

        // when
        PostPageQueryResult result = postReader.readPostPage(author.getId(), request);

        // then
        assertThat(result.posts()).extracting(PostSummaryQueryDto::postId)
                .containsExactly(post1.getId(), post3.getId(), post2.getId());
    }

    @Test
    void 상한_카운트는_상한까지만_세고_상한_미만이면_실제_개수를_반환한다() {
        // given
        Member author = memberRepository.save(Member.createSocialMember("author"));
        for (int i = 0; i < 5; i++) {
            postRepository.save(createPost(1L, author.getId(), "글" + i));
        }
        postRepository.save(createPost(2L, author.getId(), "다른 게시판"));

        PostSearchRequest request = new PostSearchRequest(1L, null, null, 1, 20, null);

        // when & then
        assertThat(postReader.countPostsUpTo(request, 3L)).isEqualTo(3L);
        assertThat(postReader.countPostsUpTo(request, 100L)).isEqualTo(5L);
    }

    @Test
    void 카테고리_필터가_있으면_상한_카운트도_카테고리를_반영한다() {
        // given
        Member author = memberRepository.save(Member.createSocialMember("author"));
        postRepository.save(createPost(1L, author.getId(), "정보글", PostCategory.INFORMATION));
        postRepository.save(createPost(1L, author.getId(), "질문글", PostCategory.QUESTION));

        PostSearchRequest request = new PostSearchRequest(1L, PostCategory.QUESTION, null, 1, 20, null);

        // when & then
        assertThat(postReader.countPostsUpTo(request, 100L)).isEqualTo(1L);
    }

    @Test
    void 키워드_상한_카운트는_목록_검색과_같은_조건으로_센다() {
        // given
        Member author = memberRepository.save(Member.createSocialMember("author"));
        postRepository.save(createPost(1L, author.getId(), "스프링 스터디"));
        postRepository.save(createPost(1L, author.getId(), "자바 스터디"));
        postRepository.save(createPost(1L, author.getId(), "스프링 모각코"));

        PostKeywordSearchRequest request = new PostKeywordSearchRequest(1L, "스프링", 1, 20);

        // when & then
        assertThat(postReader.countPostsByKeywordUpTo(request, 100L)).isEqualTo(2L);
        assertThat(postReader.countPostsByKeywordUpTo(request, 1L)).isEqualTo(1L);
    }

    @Test
    void 목록_조회는_태그가_여러_개여도_게시글당_한_건이고_태그를_모두_담는다() {
        // given
        Member author = memberRepository.save(Member.createSocialMember("author"));
        Post tagged = postRepository.save(createPost(1L, author.getId(), "태그글", List.of("태그1", "태그2", "태그3"), List.of()));
        Post plain = postRepository.save(createPost(1L, author.getId(), "무태그글"));

        PostSearchRequest request = new PostSearchRequest(1L, null, PostSortType.LATEST, 1, 10, null);

        // when
        PostPageQueryResult result = postReader.readPostPage(author.getId(), request);

        // then
        assertThat(result.posts()).extracting(PostSummaryQueryDto::postId)
                .containsExactly(plain.getId(), tagged.getId());
        assertThat(result.posts().get(1).tags()).containsExactlyInAnyOrder("태그1", "태그2", "태그3");
        assertThat(result.posts().get(0).tags()).isEmpty();
    }

    @Test
    void 썸네일은_첫번째_이미지가_사용된다() {
        // given
        Member author = memberRepository.save(Member.createSocialMember("author"));
        Post post = postRepository.save(createPost(
                1L, author.getId(), "이미지글", List.of(), List.of("첫번째.png", "두번째.png")
        ));

        PostSearchRequest request = new PostSearchRequest(1L, null, PostSortType.LATEST, 1, 10, null);

        // when
        PostPageQueryResult result = postReader.readPostPage(author.getId(), request);

        // then
        assertThat(result.posts()).hasSize(1);
        assertThat(result.posts().getFirst().thumbnailImageUrl()).isEqualTo("첫번째.png");
    }

    @Test
    void 상세_조회는_태그와_이미지를_모두_담고_이미지는_표시_순서대로_정렬된다() {
        // given
        Board board = boardRepository.save(Board.create(BoardType.DEPARTMENT, "게시판", "설명", null, 0, 1, true));
        Member author = memberRepository.save(Member.createSocialMember("author"));
        Post post = postRepository.save(createPost(
                board.getId(), author.getId(), "상세글",
                List.of("태그1", "태그2"),
                List.of("이미지1.png", "이미지2.png", "이미지3.png")
        ));

        // when
        PostDetailQueryDto detail = postReader.readPostDetail(author.getId(), post.getId());

        // then
        assertThat(detail.postId()).isEqualTo(post.getId());
        assertThat(detail.tags()).containsExactlyInAnyOrder("태그1", "태그2");
        assertThat(detail.imageUrls()).containsExactly("이미지1.png", "이미지2.png", "이미지3.png");
        assertThat(detail.isMine()).isTrue();
        assertThat(detail.authorNickname()).isEqualTo("author");
    }

    @Test
    void 비로그인_조회는_isMine이_항상_false다() {
        // given
        Member author = memberRepository.save(Member.createSocialMember("author"));
        postRepository.save(createPost(1L, author.getId(), "글"));

        PostSearchRequest request = new PostSearchRequest(1L, null, PostSortType.LATEST, 1, 10, null);

        // when
        PostPageQueryResult result = postReader.readPostPage(null, request);

        // then
        assertThat(result.posts()).hasSize(1);
        assertThat(result.posts().getFirst().isMine()).isFalse();
    }

    @Test
    void 없는_게시글_상세_조회는_NotFoundException을_던진다() {
        // given
        Member viewer = memberRepository.save(Member.createSocialMember("viewer"));

        // when & then
        assertThatThrownBy(() -> postReader.readPostDetail(viewer.getId(), 999_999L))
                .isInstanceOf(NotFoundException.class);
    }

    private Post createPost(Long boardId, Long memberId, String title) {
        return createPost(boardId, memberId, title, PostCategory.INFORMATION);
    }

    private Post createPost(Long boardId, Long memberId, String title, PostCategory category) {
        return createPost(boardId, memberId, title, category, List.of(), List.of());
    }

    private Post createPost(Long boardId, Long memberId, String title, List<String> tags, List<String> imageUrls) {
        return createPost(boardId, memberId, title, PostCategory.INFORMATION, tags, imageUrls);
    }

    private Post createPost(Long boardId, Long memberId, String title, PostCategory category,
                            List<String> tags, List<String> imageUrls) {
        return Post.createByMember(
                tags,
                imageUrls,
                boardId,
                memberId,
                title,
                title + " 본문",
                category,
                false,
                false,
                true,
                "127.0.0.1"
        );
    }
}
