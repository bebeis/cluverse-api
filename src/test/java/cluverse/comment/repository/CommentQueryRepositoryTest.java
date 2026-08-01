package cluverse.comment.repository;

import cluverse.comment.domain.Comment;
import cluverse.comment.domain.CommentPageCursor;
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
import java.time.LocalDateTime;

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
    void 개선_전후_조회가_같은_깊이_우선_페이지를_반환한다() {
        // given
        Member viewer = memberRepository.save(Member.createSocialMember("viewer"));
        Member author = memberRepository.save(Member.createSocialMember("author"));
        Member blockedAuthor = memberRepository.save(Member.createSocialMember("blocked-author"));

        Comment root1 = saveComment(10L, author.getId(), null, 0, "root-1");
        Comment child1 = saveComment(10L, author.getId(), root1, 1, "child-1");
        Comment blockedChild = saveComment(10L, blockedAuthor.getId(), root1, 1, "blocked-child");
        Comment depth2 = saveComment(10L, author.getId(), child1, 2, "depth-2");
        Comment depth3 = saveComment(10L, author.getId(), depth2, 3, "depth-3");
        Comment depth4 = saveComment(10L, author.getId(), depth3, 4, "depth-4");
        Comment depth5 = saveComment(10L, author.getId(), depth4, 5, "depth-5");
        Comment root2 = saveComment(10L, author.getId(), null, 0, "root-2");

        blockRepository.save(Block.of(viewer.getId(), blockedAuthor.getId()));
        commentLikeRepository.save(CommentLike.of(child1.getId(), viewer.getId()));

        CommentPageRequest request = new CommentPageRequest(10L, null, null, 7);
        CommentPageCursor cursor = CommentPageCursor.first(
                LocalDateTime.now().plusSeconds(1),
                commentQueryRepository.findMaxCommentId()
        );

        // when
        CommentPageQueryResult before = commentQueryRepository.findCommentPageV1(viewer.getId(), request, cursor);
        CommentPageQueryResult after = commentQueryRepository.findCommentPageV2(viewer.getId(), request, cursor);

        // then
        assertThat(before.hasNext()).isTrue();
        assertThat(after.hasNext()).isTrue();
        assertThat(before.comments()).extracting("commentId")
                .containsExactly(root1.getId(), child1.getId(), depth2.getId(), depth3.getId(), depth4.getId(),
                        depth5.getId(), blockedChild.getId())
                .doesNotContain(root2.getId());
        assertThat(after.comments()).extracting("commentId")
                .containsExactlyElementsOf(before.comments().stream().map(comment -> comment.commentId()).toList());
        assertThat(before.comments()).filteredOn(comment -> comment.commentId().equals(child1.getId()))
                .singleElement()
                .extracting("likedByMe")
                .isEqualTo(true);
        assertThat(after.comments()).filteredOn(comment -> comment.commentId().equals(blockedChild.getId()))
                .singleElement()
                .extracting("blockedAuthor")
                .isEqualTo(true);
    }

    @Test
    void 최초_조회_이후_작성된_댓글은_같은_페이지_집합에서_제외한다() {
        // given
        Member viewer = memberRepository.save(Member.createSocialMember("snapshot-viewer"));
        Member author = memberRepository.save(Member.createSocialMember("snapshot-author"));
        Comment first = saveComment(30L, author.getId(), null, 0, "first");
        Comment second = saveComment(30L, author.getId(), null, 0, "second");
        Comment third = saveComment(30L, author.getId(), null, 0, "third");
        CommentPageRequest request = new CommentPageRequest(30L, null, null, 2);
        CommentPageCursor initialCursor = CommentPageCursor.first(
                LocalDateTime.now().plusSeconds(1),
                commentQueryRepository.findMaxCommentId()
        );
        CommentPageQueryResult beforeFirstPage = commentQueryRepository.findCommentPageV1(
                viewer.getId(), request, initialCursor
        );
        CommentPageQueryResult afterFirstPage = commentQueryRepository.findCommentPageV2(
                viewer.getId(), request, initialCursor
        );
        Comment addedLater = saveComment(30L, author.getId(), null, 0, "added-later");

        // when
        CommentPageQueryResult beforeSecondPage = commentQueryRepository.findCommentPageV1(
                viewer.getId(), request, nextCursor(initialCursor, beforeFirstPage)
        );
        CommentPageQueryResult afterSecondPage = commentQueryRepository.findCommentPageV2(
                viewer.getId(), request, nextCursor(initialCursor, afterFirstPage)
        );

        // then
        assertThat(beforeFirstPage.comments()).extracting("commentId").containsExactly(first.getId(), second.getId());
        assertThat(afterFirstPage.comments()).extracting("commentId").containsExactly(first.getId(), second.getId());
        assertThat(beforeSecondPage.comments()).extracting("commentId").containsExactly(third.getId());
        assertThat(afterSecondPage.comments()).extracting("commentId").containsExactly(third.getId());
        assertThat(beforeSecondPage.comments()).extracting("commentId").doesNotContain(addedLater.getId());
        assertThat(afterSecondPage.comments()).extracting("commentId").doesNotContain(addedLater.getId());
    }

    @Test
    void 게시글별_최근_댓글_시각을_기준으로_내림차순_조회한다() throws InterruptedException {
        // given
        Member author = memberRepository.save(Member.createSocialMember("author"));

        saveComment(10L, author.getId(), null, 0, "post-10-first");
        Thread.sleep(5);
        saveComment(20L, author.getId(), null, 0, "post-20-first");
        Thread.sleep(5);
        saveComment(10L, author.getId(), null, 0, "post-10-latest");

        // when
        List<CommentLastRepliedPost> result = commentQueryRepository.findRecentCommentRepliedPosts(2L);

        // then
        assertThat(result).extracting(CommentLastRepliedPost::postId).containsExactly(10L, 20L);
        assertThat(result.getFirst().lastCommentRepliedAt()).isAfter(result.get(1).lastCommentRepliedAt());
    }

    private Comment saveComment(Long postId, Long memberId, Comment parent, int depth, String content) {
        Long parentId = parent == null ? null : parent.getId();
        Comment comment = Comment.createByMember(postId, memberId, parentId, depth, content, false, "127.0.0.1");
        Comment savedComment = commentRepository.saveAndFlush(comment);
        savedComment.assignPath(parent);
        return commentRepository.saveAndFlush(savedComment);
    }

    private CommentPageCursor nextCursor(CommentPageCursor cursor, CommentPageQueryResult result) {
        return new CommentPageCursor(result.lastPath(), cursor.asOf(), cursor.snapshotMaxCommentId());
    }
}
