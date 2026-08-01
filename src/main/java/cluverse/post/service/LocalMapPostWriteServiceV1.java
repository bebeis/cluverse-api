package cluverse.post.service;

import cluverse.place.service.implement.LocalMapMetricsRecorder;
import cluverse.post.service.implement.LocalMapPostWriteProcessorV1;
import cluverse.post.service.request.PostWithPlacesCreateRequestV1;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalMapPostWriteServiceV1 {

    private final LocalMapPostWriteProcessorV1 processor;
    private final LocalMapMetricsRecorder metricsRecorder;

    public Long create(Long memberId, PostWithPlacesCreateRequestV1 request, String clientIp) {
        return metricsRecorder.recordTransaction(
                "v1", "post", () -> processor.create(memberId, request, clientIp));
    }
}
