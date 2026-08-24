package cluverse.post.service.implement;

import cluverse.board.domain.Board;
import cluverse.board.domain.BoardType;
import cluverse.board.repository.BoardRepository;
import cluverse.common.config.QuerydslConfig;
import cluverse.common.exception.NotFoundException;
import cluverse.member.domain.Member;
import cluverse.member.repository.MemberRepository;
import cluverse.meta.domain.PostViewCount;
import cluverse.post.domain.Post;
import cluverse.post.domain.PostCategory;
import cluverse.post.repository.PostPageQueryRepository;
import cluverse.post.repository.PostQueryRepository;
import cluverse.post.repository.PostRepository;
import cluverse.post.repository.dto.PostDetailQueryDto;
import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.repository.dto.PostSummaryQueryDto;
import cluverse.post.service.request.PostKeywordSearchRequest;
import cluverse.post.service.request.PostCursorSearchRequest;
import cluverse.post.PostImageStorageTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({PostReader.class, PostPageQueryRepository.class, PostQueryRepository.class,
        QuerydslConfig.class, PostImageStorageTestConfig.class})
class PostReaderTest {

    @Autowired
    private PostReader postReader;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Test
    void 목록_조회는_태그가_여러_개여도_게시글당_한_건이고_태그를_모두_담는다() {
        // given
        Member author = memberRepository.save(Member.createSocialMember("author"));
        Post tagged = postRepository.save(createPost(1L, author.getId(), "태그글", List.of("태그1", "태그2", "태그3"), List.of()));
        Post plain = postRepository.save(createPost(1L, author.getId(), "무태그글"));

        PostCursorSearchRequest request = new PostCursorSearchRequest(1L, null, 10, null, null, null, null);

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

        PostCursorSearchRequest request = new PostCursorSearchRequest(1L, null, 10, null, null, null, null);

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

        PostCursorSearchRequest request = new PostCursorSearchRequest(1L, null, 10, null, null, null, null);

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
