package cluverse.popularity.service.implement;

import cluverse.popularity.domain.PopularityTrigger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PopularityPromotionInvokerTest {

    @Mock
    private PopularityPromotionProcessor popularityPromotionProcessor;

    @Test
    void 좋아요와_댓글_변경_직후_승격을_검사한다() {
        PopularityPromotionInvoker invoker = new PopularityPromotionInvoker(popularityPromotionProcessor);

        invoker.tryEvaluate(1L, PopularityTrigger.LIKE);
        invoker.tryEvaluateAll(List.of(1L, 2L), PopularityTrigger.COMMENT);

        verify(popularityPromotionProcessor).evaluate(1L, PopularityTrigger.LIKE);
        verify(popularityPromotionProcessor).evaluateAll(List.of(1L, 2L), PopularityTrigger.COMMENT);
    }

    @Test
    void 승격_트랜잭션의_커밋_실패가_원래_좋아요나_댓글_요청으로_전파되지_않는다() {
        // given
        doThrow(new IllegalStateException("commit failure"))
                .when(popularityPromotionProcessor).evaluate(1L, PopularityTrigger.LIKE);
        PopularityPromotionInvoker invoker = new PopularityPromotionInvoker(popularityPromotionProcessor);

        // when // then
        assertThatCode(() -> invoker.tryEvaluate(1L, PopularityTrigger.LIKE))
                .doesNotThrowAnyException();
    }
}
