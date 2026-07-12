package cluverse.post.service.implement;

import cluverse.common.config.QuerydslConfig;
import cluverse.member.domain.Member;
import cluverse.member.repository.MemberRepository;
import cluverse.post.domain.Post;
import cluverse.post.domain.PostCategory;
import cluverse.post.repository.PostPageQueryRepository;
import cluverse.post.repository.PostQueryRepository;
import cluverse.post.repository.PostRepository;
import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.repository.dto.PostSummaryQueryDto;
import cluverse.post.service.request.PostCursorDirection;
import cluverse.post.service.request.PostCursorSearchRequest;
import cluverse.post.service.request.PostOffsetSearchRequest;
import cluverse.post.service.request.PostSortType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 게시글 목록 조회 버전별(V1 naive / V4 커서) 리포지토리 동작 검증.
 * H2(MySQL 모드)는 술어의 정합성만 검증한다. 실행 계획(커버링 인덱스, range 스캔)은
 * 실제 MySQL에서 script/post-list/explain/*.sql 로 별도 확인한다.
 */
@DataJpaTest
@Import({PostReader.class, PostPageQueryRepository.class, PostQueryRepository.class, QuerydslConfig.class})
class PostReaderVersionedListTest {

    @Autowired
    private PostReader postReader;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    // ===== V1: naive offset =====

    @Test
    void V1_naive_조회는_최신순으로_오프셋_페이징된다() {
        // given
        Member author = memberRepository.save(Member.createSocialMember("author"));
        Post post1 = postRepository.save(createPost(1L, author.getId(), "글1"));
        Post post2 = postRepository.save(createPost(1L, author.getId(), "글2"));
        Post post3 = postRepository.save(createPost(1L, author.getId(), "글3"));
        Post post4 = postRepository.save(createPost(1L, author.getId(), "글4"));
        postRepository.save(createPost(2L, author.getId(), "다른 게시판"));

        PostOffsetSearchRequest request = new PostOffsetSearchRequest(1L, null, PostSortType.LATEST, 2, 2);

        // when
        PostPageQueryResult result = postReader.readPostPageWithOffset(author.getId(), request);

        // then
        assertThat(result.posts()).extracting(PostSummaryQueryDto::postId)
                .containsExactly(post2.getId(), post1.getId());
    }

    @Test
    void V1_naive_조회는_태그를_담지_않는다() {
        // given
        Member author = memberRepository.save(Member.createSocialMember("author"));
        postRepository.save(createPost(1L, author.getId(), "태그글", List.of("태그1", "태그2"), List.of()));

        PostOffsetSearchRequest request = new PostOffsetSearchRequest(1L, null, PostSortType.LATEST, 1, 10);

        // when
        PostPageQueryResult result = postReader.readPostPageWithOffset(author.getId(), request);

        // then
        assertThat(result.posts()).hasSize(1);
        assertThat(result.posts().getFirst().tags()).isEmpty();
    }

    @Test
    void 전체_카운트는_게시판과_카테고리_조건을_반영한다() {
        // given
        Member author = memberRepository.save(Member.createSocialMember("author"));
        postRepository.save(createPost(1L, author.getId(), "정보글", PostCategory.INFORMATION));
        postRepository.save(createPost(1L, author.getId(), "질문글", PostCategory.QUESTION));
        postRepository.save(createPost(2L, author.getId(), "다른 게시판", PostCategory.INFORMATION));

        // when & then
        assertThat(postReader.countPosts(new PostOffsetSearchRequest(1L, null, null, 1, 20))).isEqualTo(2L);
        assertThat(postReader.countPosts(new PostOffsetSearchRequest(1L, PostCategory.QUESTION, null, 1, 20)))
                .isEqualTo(1L);
    }

    @Test
    void V2_id_선정_조회는_V3와_같은_슬라이스를_반환한다() {
        // given
        Member author = memberRepository.save(Member.createSocialMember("author"));
        Post post1 = postRepository.save(createPost(1L, author.getId(), "글1"));
        Post post2 = postRepository.save(createPost(1L, author.getId(), "글2"));
        Post post3 = postRepository.save(createPost(1L, author.getId(), "글3"));

        PostOffsetSearchRequest request = new PostOffsetSearchRequest(1L, null, PostSortType.LATEST, 1, 2);

        // when
        PostPageQueryResult result = postReader.readPostPage(author.getId(), request);

        // then
        assertThat(result.posts()).extracting(PostSummaryQueryDto::postId)
                .containsExactly(post3.getId(), post2.getId());
        assertThat(result.hasNext()).isTrue();
    }

    // ===== V4: 날짜 앵커 + 커서 =====

    @Test
    void V4_같은_시각의_글도_NEXT_이동에서_중복과_누락이_없다() {
        // given
        Member author = memberRepository.save(Member.createSocialMember("author"));
        LocalDateTime sameTime = LocalDateTime.of(2026, 1, 20, 12, 0);
        Post post1 = savePostAt(1L, author.getId(), "글1", sameTime);
        Post post2 = savePostAt(1L, author.getId(), "글2", sameTime);
        Post post3 = savePostAt(1L, author.getId(), "글3", sameTime);
        Post post4 = savePostAt(1L, author.getId(), "글4", sameTime);
        Post post5 = savePostAt(1L, author.getId(), "글5", sameTime);

        // when: 무앵커 진입 → NEXT → NEXT
        PostPageQueryResult page1 = postReader.readPostPageByCursor(
                author.getId(), cursorRequest(1L, 2, null, null, null));
        PostPageQueryResult page2 = postReader.readPostPageByCursor(
                author.getId(), cursorRequest(1L, 2, sameTime, post4.getId(), PostCursorDirection.NEXT));
        PostPageQueryResult page3 = postReader.readPostPageByCursor(
                author.getId(), cursorRequest(1L, 2, sameTime, post2.getId(), PostCursorDirection.NEXT));

        // then: 같은 created_at에서도 post_id가 순서를 고정한다
        assertThat(page1.posts()).extracting(PostSummaryQueryDto::postId)
                .containsExactly(post5.getId(), post4.getId());
        assertThat(page1.hasNext()).isTrue();
        assertThat(page2.posts()).extracting(PostSummaryQueryDto::postId)
                .containsExactly(post3.getId(), post2.getId());
        assertThat(page3.posts()).extracting(PostSummaryQueryDto::postId)
                .containsExactly(post1.getId());
        assertThat(page3.hasNext()).isFalse();
    }

    @Test
    void V4_PREV_이동은_커서에_인접한_페이지를_최신순으로_반환한다() {
        // given
        Member author = memberRepository.save(Member.createSocialMember("author"));
        LocalDateTime sameTime = LocalDateTime.of(2026, 1, 20, 12, 0);
        Post post1 = savePostAt(1L, author.getId(), "글1", sameTime);
        Post post2 = savePostAt(1L, author.getId(), "글2", sameTime);
        Post post3 = savePostAt(1L, author.getId(), "글3", sameTime);
        Post post4 = savePostAt(1L, author.getId(), "글4", sameTime);
        Post post5 = savePostAt(1L, author.getId(), "글5", sameTime);

        // when: post1 커서 기준 최신 방향 이동
        PostPageQueryResult result = postReader.readPostPageByCursor(
                author.getId(), cursorRequest(1L, 2, sameTime, post1.getId(), PostCursorDirection.PREV));

        // then: 커서에 인접한 [글3, 글2]이 최신순으로, 그 위로 더 최신 글이 남아있다(hasNext=true)
        assertThat(result.posts()).extracting(PostSummaryQueryDto::postId)
                .containsExactly(post3.getId(), post2.getId());
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void V4_날짜_진입은_해당_날짜가_모자라면_이전_날짜_글로_이어진다() {
        // given
        Member author = memberRepository.save(Member.createSocialMember("author"));
        Post post1 = savePostAt(1L, author.getId(), "1월19일 글", LocalDateTime.of(2026, 1, 19, 9, 0));
        Post post2 = savePostAt(1L, author.getId(), "1월20일 오전 글", LocalDateTime.of(2026, 1, 20, 10, 0));
        Post post3 = savePostAt(1L, author.getId(), "1월20일 오후 글", LocalDateTime.of(2026, 1, 20, 15, 0));
        savePostAt(1L, author.getId(), "1월21일 글", LocalDateTime.of(2026, 1, 21, 9, 0));

        PostCursorSearchRequest request = new PostCursorSearchRequest(
                1L, null, 10, LocalDate.of(2026, 1, 20), null, null, null);

        // when
        PostPageQueryResult result = postReader.readPostPageByCursor(author.getId(), request);

        // then: 1/21 글은 제외되고, 1/20 글 다음에 1/19 글이 자연스럽게 이어진다
        assertThat(result.posts()).extracting(PostSummaryQueryDto::postId)
                .containsExactly(post3.getId(), post2.getId(), post1.getId());
        assertThat(postReader.existsPostsNewerThan(request)).isTrue();
    }

    @Test
    void V4_앵커보다_최신_글이_없으면_existsPostsNewerThan은_false다() {
        // given
        Member author = memberRepository.save(Member.createSocialMember("author"));
        savePostAt(1L, author.getId(), "글", LocalDateTime.of(2026, 1, 20, 10, 0));

        PostCursorSearchRequest request = new PostCursorSearchRequest(
                1L, null, 10, LocalDate.of(2026, 1, 20), null, null, null);

        // when & then
        assertThat(postReader.existsPostsNewerThan(request)).isFalse();
    }

    private PostCursorSearchRequest cursorRequest(Long boardId, int size, LocalDateTime cursorCreatedAt,
                                                  Long cursorPostId, PostCursorDirection direction) {
        return new PostCursorSearchRequest(boardId, null, size, null, cursorCreatedAt, cursorPostId, direction);
    }

    /**
     * created_at은 JPA Auditing이 채우므로, 커서 경계 검증을 위해 저장 후 native로 고정한다.
     */
    private Post savePostAt(Long boardId, Long memberId, String title, LocalDateTime createdAt) {
        Post post = postRepository.save(createPost(boardId, memberId, title));
        entityManager.createNativeQuery("UPDATE post SET created_at = :createdAt WHERE post_id = :postId")
                .setParameter("createdAt", createdAt)
                .setParameter("postId", post.getId())
                .executeUpdate();
        entityManager.clear();
        return post;
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
