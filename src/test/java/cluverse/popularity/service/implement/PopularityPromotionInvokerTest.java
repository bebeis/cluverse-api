package cluverse.popularity.service.implement;

import cluverse.popularity.domain.PopularityTrigger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class PopularityPromotionInvokerTest {

    @Mock
    private PopularityPromotionProcessorV2 popularityPromotionProcessorV2;

    @Test
    void 승격_트랜잭션의_커밋_실패가_원래_좋아요나_댓글_요청으로_전파되지_않는다() {
        // given
        doThrow(new IllegalStateException("commit failure"))
                .when(popularityPromotionProcessorV2).evaluate(1L, PopularityTrigger.LIKE);
        PopularityPromotionInvoker invoker = new PopularityPromotionInvoker(popularityPromotionProcessorV2);

        // when // then
        assertThatCode(() -> invoker.tryEvaluate(1L, PopularityTrigger.LIKE))
                .doesNotThrowAnyException();
    }
}
