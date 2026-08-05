package cluverse.meta.service.implement;

import cluverse.meta.repository.TotalViewCountRepository;
import cluverse.meta.repository.dto.ResidentViewCount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InactiveCounterEvictorTest {

    @Mock
    private TotalViewCountRepository totalViewCountRepository;

    @Mock
    private PostMetaWriter postMetaWriter;

    @InjectMocks
    private InactiveCounterEvictor inactiveCounterEvictor;

    @Test
    void 최종_체크포인트_뒤_값과_시각이_그대로일_때만_제거한다() {
        // given
        ResidentViewCount inactive = new ResidentViewCount(10L, 1_000L, 100L);
        given(totalViewCountRepository.findInactive()).willReturn(List.of(inactive));

        // when
        inactiveCounterEvictor.evict();

        // then
        verify(postMetaWriter).checkpointViewCounts(List.of(inactive.toSnapshot()));
        verify(totalViewCountRepository).deleteIfUnchanged(inactive);
    }

    @Test
    void 비활성_카운터가_없으면_DB를_호출하지_않는다() {
        // given
        given(totalViewCountRepository.findInactive()).willReturn(List.of());

        // when
        inactiveCounterEvictor.evict();

        // then
        verify(postMetaWriter, never()).checkpointViewCounts(List.of());
    }
}
