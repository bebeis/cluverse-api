package cluverse.comment.repository;

import cluverse.comment.domain.Comment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Test
    void 댓글을_쓰기_잠금으로_조회할_수_있다() {
        // given
        Comment comment = saveComment(10L, null, 0);

        // when & then
        assertThat(commentRepository.findByIdForUpdate(comment.getId()))
                .containsSame(comment);
    }

    @Test
    void 같은_게시글의_직계_자식_존재_여부를_확인한다() {
        // given
        Comment parent = saveComment(10L, null, 0);
        saveComment(10L, parent.getId(), 1);

        // when & then
        assertThat(commentRepository.existsByPostIdAndParentId(10L, parent.getId())).isTrue();
        assertThat(commentRepository.existsByPostIdAndParentId(20L, parent.getId())).isFalse();
    }

    private Comment saveComment(Long postId, Long parentId, int depth) {
        return commentRepository.saveAndFlush(Comment.createByMember(
                postId,
                1L,
                parentId,
                depth,
                "댓글",
                false,
                "127.0.0.1"
        ));
    }
}
