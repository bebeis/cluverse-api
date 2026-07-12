package cluverse.post.repository;

import cluverse.common.config.QuerydslConfig;
import cluverse.member.domain.Member;
import cluverse.member.repository.MemberRepository;
import cluverse.post.domain.Post;
import cluverse.post.domain.PostCategory;
import cluverse.post.repository.dto.PostSummaryQueryDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({PostQueryRepository.class, QuerydslConfig.class})
class PostQueryRepositoryTest {

    @Autowired
    private PostQueryRepository postQueryRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void 요약_조회는_주어진_id_순서를_그대로_유지한다() {
        // given
        Member author = memberRepository.save(Member.createSocialMember("author"));
        Post post1 = postRepository.save(createPost(1L, author.getId(), "글1"));
        Post post2 = postRepository.save(createPost(1L, author.getId(), "글2"));
        Post post3 = postRepository.save(createPost(1L, author.getId(), "글3"));

        List<Long> orderedIds = List.of(post2.getId(), post3.getId(), post1.getId());

        // when
        List<PostSummaryQueryDto> summaries = postQueryRepository.findPostSummaries(author.getId(), orderedIds);

        // then
        assertThat(summaries).extracting(PostSummaryQueryDto::postId)
                .containsExactly(post2.getId(), post3.getId(), post1.getId());
    }

    @Test
    void 요약_조회는_빈_id_목록이면_쿼리_없이_빈_목록을_반환한다() {
        // when & then
        assertThat(postQueryRepository.findPostSummaries(1L, List.of())).isEmpty();
    }

    @Test
    void 없는_게시글_상세_조회는_빈_Optional을_반환한다() {
        // when & then
        assertThat(postQueryRepository.findPostDetail(1L, 999_999L)).isEmpty();
    }

    private Post createPost(Long boardId, Long memberId, String title) {
        return Post.createByMember(
                List.of(),
                List.of(),
                boardId,
                memberId,
                title,
                title + " 본문",
                PostCategory.INFORMATION,
                false,
                false,
                true,
                "127.0.0.1"
        );
    }
}
