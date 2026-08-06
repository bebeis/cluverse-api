package cluverse.post.service.implement;

import cluverse.place.domain.SelectedPlace;
import cluverse.place.service.implement.LocalMapMetricsRecorder;
import cluverse.place.service.implement.V1PlaceSelectionResolver;
import cluverse.place.service.request.PlaceSelectionRequestV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncPostPlaceVerificationHandler {

    private final V1PlaceSelectionResolver placeSelectionResolver;
    private final PostPlaceCompletionProcessor completionProcessor;
    private final LocalMapMetricsRecorder metricsRecorder;

    @Async("localMapPlaceExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void verify(PostPlaceVerificationRequested event) {
        try {
            List<PlaceSelectionRequestV1> selections = event.places().stream()
                    .map(place -> new PlaceSelectionRequestV1(
                            place.candidate().name(),
                            place.candidate().sourceFingerprint(),
                            place.recommended()
                    ))
                    .toList();
            List<SelectedPlace> verifiedPlaces = metricsRecorder.recordAsync(
                    "provider", () -> placeSelectionResolver.resolve(selections));
            metricsRecorder.recordAsync(
                    "completion",
                    () -> completionProcessor.complete(event.memberId(), event.postId(), verifiedPlaces)
            );
        } catch (RuntimeException exception) {
            log.warn("게시글은 저장했지만 비동기 장소 검증에 실패했습니다. postId={}", event.postId(), exception);
        }
    }
}
