package cluverse.post.service;

import cluverse.place.domain.SelectedPlace;
import cluverse.place.service.implement.LocalMapMetricsRecorder;
import cluverse.place.service.implement.V2PlaceSelectionResolver;
import cluverse.post.service.implement.LocalMapPostWriteProcessorV2;
import cluverse.post.service.implement.PostAccessReader;
import cluverse.post.service.request.PostWithPlacesCreateRequestV2;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocalMapPostWriteServiceV2 {

    private final PostAccessReader postReader;
    private final V2PlaceSelectionResolver placeSelectionResolver;
    private final LocalMapPostWriteProcessorV2 processor;
    private final LocalMapMetricsRecorder metricsRecorder;

    public Long create(Long memberId, PostWithPlacesCreateRequestV2 request, String clientIp) {
        String requestId = request.requestId().toString();
        return postReader.findIdByRequestId(memberId, requestId).orElseGet(() ->
                createOnce(memberId, requestId, request, clientIp));
    }

    private Long createOnce(Long memberId, String requestId, PostWithPlacesCreateRequestV2 request, String clientIp) {
        List<SelectedPlace> selectedPlaces = placeSelectionResolver.resolve(memberId, request.places());
        try {
            return metricsRecorder.recordTransaction(
                    "v2", "post",
                    () -> processor.create(memberId, requestId, request.post(), selectedPlaces, clientIp)
            );
        } catch (DataIntegrityViolationException e) {
            return postReader.findIdByRequestId(memberId, requestId).orElseThrow(() -> e);
        }
    }
}
