package cluverse.post.service;

import cluverse.common.exception.BadRequestException;
import cluverse.post.domain.ImageUploadVersion;
import cluverse.post.domain.PostImageUpload;
import cluverse.post.domain.PostImageUploadStatus;
import cluverse.post.domain.ProcessedPostImage;
import cluverse.post.service.implement.PostImageBatchProcessor;
import cluverse.post.service.implement.PostImageStagingCleanup;
import cluverse.post.service.implement.PostImageUploadMetricsRecorder;
import cluverse.post.service.implement.PostImageUploadPreparer;
import cluverse.post.service.implement.PostImageUploadRecovery;
import cluverse.post.service.implement.PostImageUploadReservation;
import cluverse.post.service.implement.PostImageUploadReservationResult;
import cluverse.post.service.implement.PostImageUploadStorageManager;
import cluverse.post.service.implement.PostImageUploadWriter;
import cluverse.post.service.implement.PreparedPostImageUpload;
import cluverse.post.service.request.PostImageUploadRequest;
import cluverse.post.service.response.PostImageUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostImageUploadService {

    private static final ImageUploadVersion CURRENT_VERSION = ImageUploadVersion.V3;

    private final PostImageUploadPreparer imageUploadPreparer;
    private final PostImageUploadReservation reservation;
    private final PostImageUploadWriter imageUploadWriter;
    private final PostImageUploadStorageManager storageManager;
    private final PostImageUploadMetricsRecorder metricsRecorder;
    private final PostImageBatchProcessor imageBatchProcessor;
    private final PostImageUploadRecovery recovery;
    private final PostImageStagingCleanup stagingCleanup;

    public PostImageUploadResponse upload(Long memberId, PostImageUploadRequest request) {
        return metricsRecorder.recordRequest(
                CURRENT_VERSION,
                () -> execute(memberId, request),
                UploadResult::outcome
        ).response();
    }

    private UploadResult execute(Long memberId, PostImageUploadRequest request) {
        // 완료된 재요청은 multipart 임시 파일을 다시 만들지 않고 바로 반환한다.
        Optional<PostImageUpload> existing = imageUploadWriter.read(request.requestId(), CURRENT_VERSION);
        if (existing.isPresent()) {
            existing.get().validateOwner(memberId);
            return respondExisting(existing.get());
        }

        try (PreparedPostImageUpload prepared = imageUploadPreparer.prepare(CURRENT_VERSION, request)) {
            // 사전 조회 뒤에도 동시 INSERT가 가능하므로 예약 단계에서 unique 경쟁을 다시 판정한다.
            PostImageUploadReservationResult result = reservation.reserve(
                    memberId, request.requestId(), CURRENT_VERSION, prepared.assets());
            if (result instanceof PostImageUploadReservationResult.Existing existingReservation) {
                return respondExisting(existingReservation.upload());
            }
            return UploadResult.success(processCreated(result.upload(), prepared));
        }
    }

    private PostImageUploadResponse processCreated(
            PostImageUpload reserved,
            PreparedPostImageUpload prepared
    ) {
        try {
            // 이 호출은 제출된 이미지 작업이 모두 종료된 뒤 성공하거나 실패한다.
            List<ProcessedPostImage> results = imageBatchProcessor.process(prepared.images());
            PostImageUpload completed = imageUploadWriter.complete(reserved.getId(), results);
            stagingCleanup.clean(completed);
            metricsRecorder.bytes(
                    completed.getVersion(),
                    completed.getTotalSourceBytes(),
                    completed.getTotalOutputBytes()
            );
            return toResponse(completed);
        } catch (RuntimeException failure) {
            // timeout을 제외한 예약 이후 실패만 즉시 보상하고, 모호한 실패는 재조정에 맡긴다.
            recovery.afterProcessingFailure(reserved, failure);
            throw failure;
        }
    }

    PostImageUploadResponse upload(PostImageUploadRequest request) {
        return upload(null, request);
    }

    private UploadResult respondExisting(PostImageUpload upload) {
        if (upload.getStatus() == PostImageUploadStatus.COMPLETED) {
            return UploadResult.idempotent(toResponse(upload));
        }
        if (upload.getStatus() == PostImageUploadStatus.PENDING) {
            throw new BadRequestException("같은 requestId의 이미지 업로드가 진행 중입니다.");
        }
        throw new BadRequestException("실패한 requestId는 재사용할 수 없습니다.");
    }

    private PostImageUploadResponse toResponse(PostImageUpload upload) {
        return PostImageUploadResponse.of(upload, storageManager::createImageUrl);
    }

    private record UploadResult(PostImageUploadResponse response, String outcome) {

        private static UploadResult success(PostImageUploadResponse response) {
            return new UploadResult(response, "success");
        }

        private static UploadResult idempotent(PostImageUploadResponse response) {
            return new UploadResult(response, "idempotent");
        }
    }

}
