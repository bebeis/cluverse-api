package cluverse.meta.service.implement;

import cluverse.meta.properties.ViewCountProperties;
import cluverse.meta.repository.TotalViewCountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ViewCountInitializerTest {

    @Mock
    private TotalViewCountRepository totalViewCountRepository;

    @Mock
    private PostMetaReader postMetaReader;

    @Test
    void 이미_전체_카운터가_있으면_초기화_락을_건드리지_않는다() {
        given(totalViewCountRepository.read(10L)).willReturn(100L);
        ViewCountInitializer initializer = new ViewCountInitializer(
                totalViewCountRepository, postMetaReader, properties());

        long result = initializer.ensureInitialized(10L);

        assertThat(result).isEqualTo(100L);
        verify(totalViewCountRepository, never()).tryAcquireInitialization(eq(10L), anyString());
    }

    @Test
    void 락을_얻은_뒤_다시_확인하고_MySQL_값을_SET_NX한_후_자신의_토큰으로_해제한다() {
        given(totalViewCountRepository.read(10L)).willReturn(null, null, 100L);
        given(totalViewCountRepository.tryAcquireInitialization(eq(10L), anyString())).willReturn(true);
        given(postMetaReader.readViewCount(10L)).willReturn(100L);
        ViewCountInitializer initializer = new ViewCountInitializer(
                totalViewCountRepository, postMetaReader, properties());

        long result = initializer.ensureInitialized(10L);

        assertThat(result).isEqualTo(100L);
        verify(totalViewCountRepository).initializeIfAbsent(10L, 100L);
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(totalViewCountRepository).tryAcquireInitialization(eq(10L), token.capture());
        verify(totalViewCountRepository).releaseInitialization(eq(10L), token.capture());
        assertThat(token.getAllValues()).hasSize(2);
        assertThat(token.getAllValues().get(0)).isEqualTo(token.getAllValues().get(1));
    }

    private ViewCountProperties properties() {
        return new ViewCountProperties(
                Duration.ofMinutes(30),
                Duration.ofMinutes(1),
                Duration.ofMinutes(30),
                1000,
                1000,
                Duration.ofSeconds(1),
                Duration.ZERO,
                3
        );
    }
}
