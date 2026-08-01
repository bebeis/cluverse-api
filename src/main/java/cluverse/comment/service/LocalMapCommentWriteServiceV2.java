package cluverse.comment.service;

import cluverse.comment.service.implement.LocalMapCommentReader;
import cluverse.comment.service.implement.LocalMapCommentWriteProcessorV2;
import cluverse.comment.service.request.CommentWithPlaceCreateRequestV2;
import cluverse.place.domain.SelectedPlace;
import cluverse.place.service.implement.LocalMapMetricsRecorder;
import cluverse.place.service.implement.V2PlaceSelectionResolver;
import cluverse.popularity.domain.PopularityTrigger;
import cluverse.popularity.service.implement.PopularityPromotionInvoker;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocalMapCommentWriteServiceV2 {

    private final LocalMapCommentReader commentReader;
    private final V2PlaceSelectionResolver placeSelectionResolver;
    private final LocalMapCommentWriteProcessorV2 processor;
    private final LocalMapMetricsRecorder metricsRecorder;
    private final PopularityPromotionInvoker popularityPromotionInvoker;

    public Long create(Long memberId, Long postId, CommentWithPlaceCreateRequestV2 request, String clientIp) {
        String requestId = request.requestId().toString();
        Long existingId = commentReader.findIdByRequestId(memberId, requestId).orElse(null);
        if (existingId != null) {
            return existingId;
        }
        SelectedPlace selected = placeSelectionResolver.resolve(memberId, List.of(request.place())).getFirst();
        try {
            Long commentId = metricsRecorder.recordTransaction(
                    "v2", "comment",
                    () -> processor.create(memberId, postId, requestId, request.comment(), selected, clientIp)
            );
            popularityPromotionInvoker.tryEvaluate(postId, PopularityTrigger.COMMENT);
            return commentId;
        } catch (DataIntegrityViolationException e) {
            return commentReader.findIdByRequestId(memberId, requestId).orElseThrow(() -> e);
        }
    }
}
