package cluverse.post.service;

import cluverse.common.exception.BadRequestException;
import cluverse.post.domain.ImageUploadVersion;
import cluverse.post.domain.PostImageUpload;
import cluverse.post.domain.PostImageUploadStatus;
import cluverse.post.domain.ProcessedPostImage;
import cluverse.post.exception.PostImageUploadTimeoutException;
import cluverse.post.service.implement.PostImageUploadExperimentAuthorizer;
import cluverse.post.service.implement.PostImageUploadMetricsRecorder;
import cluverse.post.service.implement.PostImageUploadPreparer;
import cluverse.post.service.implement.PostImageUploadProcessor;
import cluverse.post.service.implement.PostImageUploadReservation;
import cluverse.post.service.implement.PostImageUploadReservationResult;
import cluverse.post.service.implement.PostImageUploadStorageManager;
import cluverse.post.service.implement.PostImageUploadWriter;
import cluverse.post.service.implement.PreparedPostImageUpload;
import cluverse.post.service.request.ImageUploadFailurePoint;
import cluverse.post.service.request.PostImageUploadRequest;
import cluverse.post.service.response.PostImageUploadResponse;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PostImageUploadService {

    private final PostImageUploadExperimentAuthorizer authorizer;
    private final PostImageUploadPreparer preparer;
    private final PostImageUploadReservation reservation;
    private final PostImageUploadWriter writer;
    private final PostImageUploadStorageManager storageManager;
    private final PostImageUploadMetricsRecorder metricsRecorder;
    private final Map<ImageUploadVersion, PostImageUploadProcessor> processors;

    public PostImageUploadService(
            PostImageUploadExperimentAuthorizer authorizer,
            PostImageUploadPreparer preparer,
            PostImageUploadReservation reservation,
            PostImageUploadWriter writer,
            PostImageUploadStorageManager storageManager,
            PostImageUploadMetricsRecorder metricsRecorder,
            List<PostImageUploadProcessor> processors
    ) {
        this.authorizer = authorizer;
        this.preparer = preparer;
        this.reservation = reservation;
        this.writer = writer;
        this.storageManager = storageManager;
        this.metricsRecorder = metricsRecorder;
        this.processors = new EnumMap<>(ImageUploadVersion.class);
        processors.forEach(processor -> this.processors.put(processor.version(), processor));
    }

    public PostImageUploadResponse upload(
            ImageUploadVersion version,
            String benchmarkToken,
            PostImageUploadRequest request
    ) {
        authorizer.authorize(benchmarkToken);
        PostImageUploadProcessor processor = readProcessor(version);
        long startedAt = System.nanoTime();
        PostImageUpload reserved = null;
        boolean ownsReservation = false;

        try {
            Optional<PostImageUpload> existing = writer.read(request.requestId(), version);
            if (existing.isPresent()) {
                return respondExisting(existing.get(), startedAt);
            }

            try (PreparedPostImageUpload prepared = preparer.prepare(version, request)) {
                PostImageUploadReservationResult reservationResult = reservation.reserve(
                        request.requestId(), version, prepared.assets());
                reserved = reservationResult.upload();
                ownsReservation = reservationResult.created();
                if (!ownsReservation) {
                    return respondExisting(reserved, startedAt);
                }

                List<ProcessedPostImage> results = processor.process(prepared.images(), request.failurePoint());
                injectBeforeDatabaseFailure(request.failurePoint());
                PostImageUpload completed = writer.complete(reserved.getId(), results);
                cleanupStaging(completed);
                metricsRecorder.bytes(version, completed.getTotalSourceBytes(), completed.getTotalOutputBytes());
                metricsRecorder.request(version, "success", System.nanoTime() - startedAt);
                return PostImageUploadResponse.of(completed);
            }
        } catch (RuntimeException exception) {
            if (ownsReservation && !(exception instanceof PostImageUploadTimeoutException)) {
                compensate(reserved, exception);
            }
            String outcome = exception instanceof PostImageUploadTimeoutException ? "timeout" : "failure";
            metricsRecorder.request(version, outcome, System.nanoTime() - startedAt);
            throw exception;
        }
    }

    private PostImageUploadProcessor readProcessor(ImageUploadVersion version) {
        PostImageUploadProcessor processor = processors.get(version);
        if (processor == null) {
            throw new BadRequestException("지원하지 않는 이미지 업로드 버전입니다.");
        }
        return processor;
    }

    private PostImageUploadResponse respondExisting(PostImageUpload upload, long startedAt) {
        if (upload.getStatus() == PostImageUploadStatus.COMPLETED) {
            metricsRecorder.request(upload.getVersion(), "idempotent", System.nanoTime() - startedAt);
            return PostImageUploadResponse.of(upload);
        }
        if (upload.getStatus() == PostImageUploadStatus.PENDING) {
            throw new BadRequestException("같은 requestId의 이미지 업로드가 진행 중입니다.");
        }
        throw new BadRequestException("실패한 requestId는 재사용할 수 없습니다.");
    }

    private void injectBeforeDatabaseFailure(ImageUploadFailurePoint failurePoint) {
        if (failurePoint == ImageUploadFailurePoint.BEFORE_DATABASE_COMPLETE) {
            throw new IllegalStateException("DB 확정 직전 실패 주입");
        }
    }

    private void cleanupStaging(PostImageUpload completed) {
        if (storageManager.deleteStaging(completed)) {
            writer.markStagingCleaned(completed.getId());
            completed.markStagingCleaned();
        }
    }

    private void compensate(PostImageUpload upload, RuntimeException failure) {
        if (!storageManager.compensate(upload)) {
            return;
        }
        try {
            writer.fail(upload.getId(), failure.getMessage());
        } catch (RuntimeException ignored) {
            // PENDING 기록이 재조정 기준점으로 남는다.
        }
    }

}
