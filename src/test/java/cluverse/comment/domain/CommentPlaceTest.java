package cluverse.comment.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommentPlaceTest {

    @Test
    void 댓글이_장소_연관관계를_직접_추가한다() {
        Comment comment = Comment.createByMember(1L, 2L, null, 0, "내용", false, "127.0.0.1");

        comment.attachPlace(3L, 4L, 5L, true);

        assertThat(comment.getPlace().getComment()).isSameAs(comment);
        assertThat(comment.getPlace().getPlaceId()).isEqualTo(3L);
        assertThat(comment.getPlace().isRecommended()).isTrue();
    }
}
