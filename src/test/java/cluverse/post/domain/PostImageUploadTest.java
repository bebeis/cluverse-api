package cluverse.post.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostImageUploadTest {

    @Test
    void 예약된_업로드는_모든_이미지_결과와_함께_완료된다() {
        PostImageUpload upload = PostImageUpload.reserve(
                UUID.randomUUID(), ImageUploadVersion.V3, List.of(
                        PostImageAsset.plan(0, "staging/a", "content/a.jpg", "thumbnail/a.jpg", 1_000)
                )
        );

        upload.complete(List.of(new ProcessedPostImage(
                0,
                new PostImageMetadata("content/a.jpg", "image/jpeg", 1280, 720, 400),
                new PostImageMetadata("thumbnail/a.jpg", "image/jpeg", 320, 180, 80)
        )));

        assertThat(upload.getStatus()).isEqualTo(PostImageUploadStatus.COMPLETED);
        assertThat(upload.getAssets().getFirst().getContentBytes()).isEqualTo(400);
        assertThat(upload.getAssets().getFirst().getThumbnailBytes()).isEqualTo(80);
        assertThat(upload.getTotalSourceBytes()).isEqualTo(1_000);
        assertThat(upload.getTotalOutputBytes()).isEqualTo(480);
    }

    @Test
    void 완료된_업로드를_실패로_되돌릴_수_없다() {
        PostImageUpload upload = PostImageUpload.completed(
                UUID.randomUUID(), ImageUploadVersion.V1, List.of(
                        PostImageAsset.completedOriginal(0, "content/a.jpg", "image/jpeg", 100)
                )
        );

        assertThatThrownBy(() -> upload.fail("late failure"))
                .isInstanceOf(IllegalStateException.class);
    }
}
