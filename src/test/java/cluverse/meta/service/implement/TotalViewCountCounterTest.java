package cluverse.meta.service.implement;

import cluverse.meta.repository.TotalViewCountRepository;
import cluverse.meta.repository.dto.TotalViewCountResult;
import cluverse.meta.repository.dto.TotalViewCountStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TotalViewCountCounterTest {

    @Mock
    private TotalViewCountRepository totalViewCountRepository;

    @Mock
    private ViewCountInitializer viewCountInitializer;

    @Mock
    private LocalViewCountFallback localViewCountFallback;

    @InjectMocks
    private TotalViewCountCounter totalViewCountCounter;

    @Test
    void 제거_경쟁으로_카운터가_사라지면_재초기화한_뒤_다시_집계한다() {
        // given
        Long postId = 10L;
        String cookieId = "cookie-1";
        given(totalViewCountRepository.count(postId, cookieId))
                .willReturn(new TotalViewCountResult(TotalViewCountStatus.REINITIALIZE, 0L))
                .willReturn(new TotalViewCountResult(TotalViewCountStatus.COUNTED, 101L));

        // when
        ViewCountResult result = totalViewCountCounter.count(postId, cookieId);

        // then
        assertThat(result.viewCount()).isEqualTo(101L);
        assertThat(result.counted()).isTrue();
        verify(viewCountInitializer).ensureInitialized(postId);
    }

    @Test
    void 중복_방지_락이_이미_있으면_현재값만_반환한다() {
        // given
        Long postId = 10L;
        String cookieId = "cookie-1";
        given(totalViewCountRepository.count(postId, cookieId))
                .willReturn(new TotalViewCountResult(TotalViewCountStatus.DUPLICATE, 100L));

        // when
        ViewCountResult result = totalViewCountCounter.count(postId, cookieId);

        // then
        assertThat(result.viewCount()).isEqualTo(100L);
        assertThat(result.counted()).isFalse();
    }
}
