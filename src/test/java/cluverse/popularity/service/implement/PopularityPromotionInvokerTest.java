package cluverse.popularity.service.implement;

import cluverse.popularity.domain.PopularityTrigger;
import cluverse.popularity.properties.PopularityInlineEvaluationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PopularityPromotionInvokerTest {

    @Mock
    private PopularityPromotionProcessorV2 popularityPromotionProcessorV2;

    @Test
    void 인라인_판정이_비활성화되면_단건과_배치_판정을_건너뛴다() {
        PopularityPromotionInvoker invoker = new PopularityPromotionInvoker(
                popularityPromotionProcessorV2,
                new PopularityInlineEvaluationProperties(false)
        );

        invoker.tryEvaluate(1L, PopularityTrigger.LIKE);
        invoker.tryEvaluateAll(List.of(1L, 2L), PopularityTrigger.COMMENT);

        verifyNoInteractions(popularityPromotionProcessorV2);
    }

    @Test
    void 인라인_판정이_활성화되면_기존_V2_판정을_실행한다() {
        PopularityPromotionInvoker invoker = new PopularityPromotionInvoker(
                popularityPromotionProcessorV2,
                new PopularityInlineEvaluationProperties(true)
        );

        invoker.tryEvaluate(1L, PopularityTrigger.LIKE);
        invoker.tryEvaluateAll(List.of(1L, 2L), PopularityTrigger.COMMENT);

        verify(popularityPromotionProcessorV2).evaluate(1L, PopularityTrigger.LIKE);
        verify(popularityPromotionProcessorV2).evaluateAll(List.of(1L, 2L), PopularityTrigger.COMMENT);
    }

    @Test
    void 승격_트랜잭션의_커밋_실패가_원래_좋아요나_댓글_요청으로_전파되지_않는다() {
        // given
        doThrow(new IllegalStateException("commit failure"))
                .when(popularityPromotionProcessorV2).evaluate(1L, PopularityTrigger.LIKE);
        PopularityPromotionInvoker invoker = new PopularityPromotionInvoker(
                popularityPromotionProcessorV2,
                new PopularityInlineEvaluationProperties(true)
        );

        // when // then
        assertThatCode(() -> invoker.tryEvaluate(1L, PopularityTrigger.LIKE))
                .doesNotThrowAnyException();
    }
}
