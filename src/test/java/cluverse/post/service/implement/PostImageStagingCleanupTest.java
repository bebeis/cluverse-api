package cluverse.post.service.implement;

import cluverse.post.domain.PostImageUpload;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostImageStagingCleanupTest {

    @Test
    void staging_객체를_삭제한_뒤_DB에_정리_완료를_기록한다() {
        PostImageUploadStorageManager storage = mock(PostImageUploadStorageManager.class);
        PostImageUploadWriter writer = mock(PostImageUploadWriter.class);
        PostImageUpload upload = upload(1L);
        PostImageStagingCleanup cleanup = new PostImageStagingCleanup(storage, writer);

        PostImageCleanupOutcome outcome = cleanup.clean(upload);

        org.assertj.core.api.Assertions.assertThat(outcome).isEqualTo(PostImageCleanupOutcome.COMPLETED);
        var ordered = inOrder(storage, writer);
        ordered.verify(storage).deleteStaging(upload);
        ordered.verify(writer).markStagingCleaned(1L);
    }

    @Test
    void staging_삭제에_실패하면_DB에는_미정리_상태를_남긴다() {
        PostImageUploadStorageManager storage = mock(PostImageUploadStorageManager.class);
        PostImageUploadWriter writer = mock(PostImageUploadWriter.class);
        PostImageUpload upload = upload(1L);
        doThrow(new IllegalStateException("delete failed")).when(storage).deleteStaging(upload);
        PostImageStagingCleanup cleanup = new PostImageStagingCleanup(storage, writer);

        PostImageCleanupOutcome outcome = cleanup.clean(upload);

        org.assertj.core.api.Assertions.assertThat(outcome).isEqualTo(PostImageCleanupOutcome.DEFERRED);
        verify(writer, never()).markStagingCleaned(1L);
    }

    private PostImageUpload upload(Long id) {
        PostImageUpload upload = mock(PostImageUpload.class);
        when(upload.getId()).thenReturn(id);
        return upload;
    }
}
