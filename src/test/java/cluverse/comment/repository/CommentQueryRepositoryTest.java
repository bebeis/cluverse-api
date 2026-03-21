package cluverse.comment.repository;

import cluverse.comment.domain.Comment;
import cluverse.comment.service.response.CommentLastRepliedPost;
import cluverse.comment.repository.dto.CommentPageQueryResult;
import cluverse.comment.service.request.CommentPageRequest;
import cluverse.common.config.QuerydslConfig;
import cluverse.member.domain.Block;
import cluverse.member.domain.Member;
import cluverse.member.repository.BlockRepository;
import cluverse.member.repository.MemberRepository;
import cluverse.reaction.domain.CommentLike;
import cluverse.reaction.repository.CommentLikeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({CommentQueryRepository.class, QuerydslConfig.class})
class CommentQueryRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentQueryRepository commentQueryRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Test
    void 루트_댓글을_기준으로_재귀_트리를_조회하고_depth_5까지만_가져온다() {
        // given
        Member viewer = memberRepository.save(Member.createSocialMember("viewer"));
        Member author = memberRepository.save(Member.createSocialMember("author"));
        Member blockedAuthor = memberRepository.save(Member.createSocialMember("blocked-author"));

        Comment root1 = commentRepository.save(createComment(10L, author.getId(), null, 0, "root-1"));
        Comment child1 = commentRepository.save(createComment(10L, author.getId(), root1.getId(), 1, "child-1"));
        Comment blockedChild = commentRepository.save(createComment(10L, blockedAuthor.getId(), root1.getId(), 1, "blocked-child"));
        Comment depth2 = commentRepository.save(createComment(10L, author.getId(), child1.getId(), 2, "depth-2"));
        Comment depth3 = commentRepository.save(createComment(10L, author.getId(), depth2.getId(), 3, "depth-3"));
        Comment depth4 = commentRepository.save(createComment(10L, author.getId(), depth3.getId(), 4, "depth-4"));
        Comment depth5 = commentRepository.save(createComment(10L, author.getId(), depth4.getId(), 5, "depth-5"));
        Comment depth6 = commentRepository.save(createComment(10L, author.getId(), depth5.getId(), 6, "depth-6"));
        Comment root2 = commentRepository.save(createComment(10L, author.getId(), null, 0, "root-2"));

        blockRepository.save(Block.of(viewer.getId(), blockedAuthor.getId()));
        commentLikeRepository.save(CommentLike.of(child1.getId(), viewer.getId()));

        CommentPageRequest request = new CommentPageRequest(10L, null, 0, 1);

        // when
        CommentPageQueryResult result = commentQueryRepository.findCommentPage(viewer.getId(), request);

        // then
        assertThat(result.hasNext()).isTrue();
        assertThat(result.comments()).extracting("commentId")
                .contains(root1.getId(), child1.getId(), blockedChild.getId(), depth2.getId(), depth3.getId(), depth4.getId(), depth5.getId())
                .doesNotContain(depth6.getId(), root2.getId());
        assertThat(result.comments()).filteredOn(comment -> comment.commentId().equals(child1.getId()))
                .singleElement()
                .extracting("likedByMe")
                .isEqualTo(true);
        assertThat(result.comments()).filteredOn(comment -> comment.commentId().equals(blockedChild.getId()))
                .singleElement()
                .extracting("blockedAuthor")
                .isEqualTo(true);
    }

    @Test
    void 게시글별_최근_댓글_시각을_기준으로_내림차순_조회한다() throws InterruptedException {
        // given
        Member author = memberRepository.save(Member.createSocialMember("author"));

        commentRepository.save(createComment(10L, author.getId(), null, 0, "post-10-first"));
        Thread.sleep(5);
        commentRepository.save(createComment(20L, author.getId(), null, 0, "post-20-first"));
        Thread.sleep(5);
        commentRepository.save(createComment(10L, author.getId(), null, 0, "post-10-latest"));

        // when
        List<CommentLastRepliedPost> result = commentQueryRepository.findRecentCommentRepliedPosts(2L);

        // then
        assertThat(result).extracting(CommentLastRepliedPost::postId).containsExactly(10L, 20L);
        assertThat(result.getFirst().lastCommentRepliedAt()).isAfter(result.get(1).lastCommentRepliedAt());
    }

    private Comment createComment(Long postId, Long memberId, Long parentId, int depth, String content) {
        return Comment.createByMember(postId, memberId, parentId, depth, content, false, "127.0.0.1");
    }
}
