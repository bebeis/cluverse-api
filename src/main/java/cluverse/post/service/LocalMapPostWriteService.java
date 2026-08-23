package cluverse.post.service;

import cluverse.place.domain.SelectedPlace;
import cluverse.place.service.implement.LocalMapMetricsRecorder;
import cluverse.place.service.implement.PlaceSelectionResolver;
import cluverse.post.service.implement.LocalMapPostWriteProcessor;
import cluverse.post.service.implement.PostAccessReader;
import cluverse.post.service.request.PostWithPlacesCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocalMapPostWriteService {

    private final PostAccessReader postReader;
    private final PlaceSelectionResolver placeSelectionResolver;
    private final LocalMapPostWriteProcessor processor;
    private final LocalMapMetricsRecorder metricsRecorder;

    public Long create(Long memberId, PostWithPlacesCreateRequest request, String clientIp) {
        String requestId = request.requestId().toString();
        return postReader.findIdByRequestId(memberId, requestId).orElseGet(() ->
                createOnce(memberId, requestId, request, clientIp));
    }

    private Long createOnce(Long memberId, String requestId, PostWithPlacesCreateRequest request, String clientIp) {
        List<SelectedPlace> selectedPlaces = placeSelectionResolver.resolve(memberId, request.places());
        try {
            return metricsRecorder.recordTransaction(
                    "current", "post",
                    () -> processor.create(memberId, requestId, request.post(), selectedPlaces, clientIp)
            );
        } catch (DataIntegrityViolationException e) {
            return postReader.findIdByRequestId(memberId, requestId).orElseThrow(() -> e);
        }
    }
}
