package cluverse.post.service.implement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PostImageUploadTemporaryFileCleanerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void 즉시_삭제하지_못한_경로를_다음_정리_주기에_재시도한다() throws IOException {
        PostImageUploadMetricsRecorder metrics = mock(PostImageUploadMetricsRecorder.class);
        PostImageUploadTemporaryFileCleaner cleaner = new PostImageUploadTemporaryFileCleaner(metrics);
        Path deferredDirectory = Files.createDirectory(temporaryDirectory.resolve("deferred"));
        Path child = Files.writeString(deferredDirectory.resolve("active.source"), "source");

        cleaner.delete(deferredDirectory);
        Files.delete(child);
        cleaner.retryDeferredDeletes();

        assertThat(deferredDirectory).doesNotExist();
        verify(metrics).temporaryFileCleanup("deferred");
        verify(metrics).temporaryFileCleanup("retry_deleted");
    }
}
