package cluverse.popularity.service.implement;

import cluverse.popularity.domain.PopularityTrigger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PopularityPromotionInvoker {

    private final PopularityPromotionProcessor popularityPromotionProcessor;

    public void tryEvaluate(Long postId, PopularityTrigger trigger) {
        try {
            popularityPromotionProcessor.evaluate(postId, trigger);
        } catch (Exception exception) {
            log.warn("인기글 승격 검사 실패: postId={}, trigger={}", postId, trigger, exception);
        }
    }

    public void tryEvaluateAll(List<Long> postIds, PopularityTrigger trigger) {
        try {
            popularityPromotionProcessor.evaluateAll(postIds, trigger);
        } catch (Exception exception) {
            log.warn("인기글 배치 승격 검사 실패: size={}, trigger={}", postIds.size(), trigger, exception);
        }
    }
}
