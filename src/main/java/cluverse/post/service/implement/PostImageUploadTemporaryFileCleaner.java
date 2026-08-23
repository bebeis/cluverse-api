package cluverse.post.service.implement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostImageUploadTemporaryFileCleaner {

    private final PostImageUploadMetricsRecorder metricsRecorder;
    private final Queue<Path> deferredDeletes = new ConcurrentLinkedQueue<>();

    public void delete(Path path) {
        try {
            Files.deleteIfExists(path);
            metricsRecorder.temporaryFileCleanup("deleted");
        } catch (IOException exception) {
            deferredDeletes.offer(path);
            metricsRecorder.temporaryFileCleanup("deferred");
            log.warn("이미지 업로드 임시 파일 삭제를 연기합니다. path={}", path, exception);
        }
    }

    @Scheduled(fixedDelayString = "${post-image-upload.cleanup-interval:30s}")
    void retryDeferredDeletes() {
        int retryCount = deferredDeletes.size();
        for (int index = 0; index < retryCount; index++) {
            Path path = deferredDeletes.poll();
            if (path == null) {
                return;
            }
            try {
                Files.deleteIfExists(path);
                metricsRecorder.temporaryFileCleanup("retry_deleted");
            } catch (IOException exception) {
                deferredDeletes.offer(path);
                metricsRecorder.temporaryFileCleanup("retry_failed");
                log.warn("이미지 업로드 임시 파일 삭제 재시도에 실패했습니다. path={}", path, exception);
            }
        }
    }
}
