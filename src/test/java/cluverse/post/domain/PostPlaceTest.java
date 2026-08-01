package cluverse.post.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostPlaceTest {

    @Test
    void 게시글이_장소_연관관계를_직접_추가한다() {
        Post post = Post.createByMember(
                List.of(), List.of(), 1L, 2L, "제목", "내용", PostCategory.INFORMATION,
                false, false, true, "127.0.0.1");

        post.addPlace(3L, 0, 4L, 5L, true);

        assertThat(post.getPlaces()).singleElement().satisfies(place -> {
            assertThat(place.getPost()).isSameAs(post);
            assertThat(place.getPlaceId()).isEqualTo(3L);
            assertThat(place.getDisplayOrder()).isZero();
            assertThat(place.isRecommended()).isTrue();
        });
    }
}
