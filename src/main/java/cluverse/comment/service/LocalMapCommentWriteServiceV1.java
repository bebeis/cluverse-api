package cluverse.comment.service;

import cluverse.comment.service.implement.LocalMapCommentWriteProcessorV1;
import cluverse.comment.service.request.CommentWithPlaceCreateRequestV1;
import cluverse.place.service.implement.LocalMapMetricsRecorder;
import cluverse.popularity.domain.PopularityTrigger;
import cluverse.popularity.service.implement.PopularityPromotionInvoker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalMapCommentWriteServiceV1 {

    private final LocalMapCommentWriteProcessorV1 processor;
    private final LocalMapMetricsRecorder metricsRecorder;
    private final PopularityPromotionInvoker popularityPromotionInvoker;

    public Long create(Long memberId, Long postId, CommentWithPlaceCreateRequestV1 request, String clientIp) {
        Long commentId = metricsRecorder.recordTransaction(
                "v1", "comment", () -> processor.create(memberId, postId, request, clientIp));
        popularityPromotionInvoker.tryEvaluate(postId, PopularityTrigger.COMMENT);
        return commentId;
    }
}
