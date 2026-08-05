package cluverse.post.service.implement;

import cluverse.post.domain.ImageUploadVersion;
import cluverse.post.domain.PostImageAsset;
import cluverse.post.domain.PostImageUpload;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostImageUploadReservationTest {

    @Test
    void 완료된_requestId가_재시도되면_기존_예약을_반환한다() {
        PostImageUploadWriter writer = mock(PostImageUploadWriter.class);
        PostImageUploadReservation reservation = new PostImageUploadReservation(writer);
        UUID requestId = UUID.randomUUID();
        PostImageUpload completed = PostImageUpload.completed(
                requestId, ImageUploadVersion.V3,
                List.of(PostImageAsset.completedOriginal(0, "content/a.jpg", "image/jpeg", 1))
        );
        when(writer.read(requestId, ImageUploadVersion.V3)).thenReturn(Optional.of(completed));

        PostImageUploadReservationResult result = reservation.reserve(
                requestId,
                ImageUploadVersion.V3,
                List.of(PostImageAsset.plan(0, "staging/a", "content/a.jpg", "thumbnail/a.jpg", 1))
        );

        assertThat(result.upload()).isSameAs(completed);
        assertThat(result.created()).isFalse();
        verify(writer).read(requestId, ImageUploadVersion.V3);
    }
}
