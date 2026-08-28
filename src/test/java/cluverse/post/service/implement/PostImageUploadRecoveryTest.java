package cluverse.post.service.implement;

import cluverse.post.domain.PostImageUpload;
import cluverse.post.exception.PostImageUploadTimeoutException;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostImageUploadRecoveryTest {

    @Test
    void 일반_실패는_객체를_삭제한_뒤_FAILED로_기록한다() {
        PostImageUploadStorageManager storage = mock(PostImageUploadStorageManager.class);
        PostImageUploadWriter writer = mock(PostImageUploadWriter.class);
        PostImageUpload upload = upload(1L);
        PostImageUploadRecovery recovery = recovery(storage, writer);

        recovery.afterProcessingFailure(upload, new IllegalStateException("boom"));

        var ordered = inOrder(storage, writer);
        ordered.verify(storage).deleteAll(upload);
        ordered.verify(writer).fail(1L, "boom");
    }

    @Test
    void timeout은_늦은_외부_작업과_경쟁하지_않도록_즉시_보상하지_않는다() {
        PostImageUploadStorageManager storage = mock(PostImageUploadStorageManager.class);
        PostImageUploadWriter writer = mock(PostImageUploadWriter.class);
        PostImageUpload upload = upload(1L);
        PostImageUploadRecovery recovery = recovery(storage, writer);

        recovery.afterProcessingFailure(
                upload,
                new PostImageUploadTimeoutException("timeout", new IllegalStateException())
        );

        verify(storage, never()).deleteAll(upload);
        verify(writer, never()).fail(1L, "timeout");
    }

    @Test
    void 객체_삭제에_실패하면_PENDING_기록을_남긴다() {
        PostImageUploadStorageManager storage = mock(PostImageUploadStorageManager.class);
        PostImageUploadWriter writer = mock(PostImageUploadWriter.class);
        PostImageUpload upload = upload(1L);
        doThrow(new IllegalStateException("delete failed")).when(storage).deleteAll(upload);
        PostImageUploadRecovery recovery = recovery(storage, writer);

        recovery.afterProcessingFailure(upload, new IllegalStateException("boom"));

        verify(writer, never()).fail(1L, "boom");
    }

    @Test
    void 점유한_재조정_대상은_객체를_삭제한_뒤_보상을_완료한다() {
        PostImageUploadStorageManager storage = mock(PostImageUploadStorageManager.class);
        PostImageUploadWriter writer = mock(PostImageUploadWriter.class);
        PostImageUploadRecoveryStore recoveryStore = mock(PostImageUploadRecoveryStore.class);
        PostImageUpload upload = upload(1L);
        PostImageUploadRecovery recovery = new PostImageUploadRecovery(storage, writer, recoveryStore);

        PostImageCleanupOutcome outcome = recovery.compensateClaimed(upload, "stale pending reconciled");

        org.assertj.core.api.Assertions.assertThat(outcome).isEqualTo(PostImageCleanupOutcome.COMPLETED);
        var ordered = inOrder(storage, recoveryStore);
        ordered.verify(storage).deleteAll(upload);
        ordered.verify(recoveryStore).completeCompensation(1L, "stale pending reconciled");
    }

    @Test
    void 점유한_재조정_대상의_삭제가_실패하면_재시도를_위해_연기한다() {
        PostImageUploadStorageManager storage = mock(PostImageUploadStorageManager.class);
        PostImageUploadWriter writer = mock(PostImageUploadWriter.class);
        PostImageUploadRecoveryStore recoveryStore = mock(PostImageUploadRecoveryStore.class);
        PostImageUpload upload = upload(1L);
        doThrow(new IllegalStateException("delete failed")).when(storage).deleteAll(upload);
        PostImageUploadRecovery recovery = new PostImageUploadRecovery(storage, writer, recoveryStore);

        PostImageCleanupOutcome outcome = recovery.compensateClaimed(upload, "stale pending reconciled");

        org.assertj.core.api.Assertions.assertThat(outcome).isEqualTo(PostImageCleanupOutcome.DEFERRED);
        verify(recoveryStore, never()).completeCompensation(1L, "stale pending reconciled");
    }

    private PostImageUpload upload(Long id) {
        PostImageUpload upload = mock(PostImageUpload.class);
        when(upload.getId()).thenReturn(id);
        return upload;
    }

    private PostImageUploadRecovery recovery(
            PostImageUploadStorageManager storage,
            PostImageUploadWriter writer
    ) {
        return new PostImageUploadRecovery(storage, writer, mock(PostImageUploadRecoveryStore.class));
    }
}
