package cluverse.post.service;

import cluverse.place.domain.SelectedPlace;
import cluverse.place.service.implement.LocalMapMetricsRecorder;
import cluverse.place.service.implement.PlaceSelectionResolver;
import cluverse.post.service.implement.LocalMapPostWriteProcessor;
import cluverse.post.service.implement.PostAccessReader;
import cluverse.post.service.request.PostWithPlacesCreateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocalMapPostWriteServiceTest {

    @Mock
    private PostAccessReader postReader;
    @Mock
    private PlaceSelectionResolver placeSelectionResolver;
    @Mock
    private LocalMapPostWriteProcessor processor;
    @Mock
    private LocalMapMetricsRecorder metricsRecorder;
    @InjectMocks
    private LocalMapPostWriteService service;

    @Test
    void 같은_requestId가_이미_처리됐으면_토큰을_다시_검증하지_않는다() {
        UUID requestId = UUID.randomUUID();
        PostWithPlacesCreateRequest request = org.mockito.Mockito.mock(PostWithPlacesCreateRequest.class);
        given(request.requestId()).willReturn(requestId);
        given(postReader.findIdByRequestId(1L, requestId.toString())).willReturn(Optional.of(42L));

        Long result = service.create(1L, request, "127.0.0.1");

        assertThat(result).isEqualTo(42L);
        verify(placeSelectionResolver, never()).resolve(any(), any());
        verify(processor, never()).create(any(), any(), any(), any(), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void 토큰_검증을_끝낸_결과만_트랜잭션_프로세서에_전달한다() {
        UUID requestId = UUID.randomUUID();
        PostWithPlacesCreateRequest request = org.mockito.Mockito.mock(PostWithPlacesCreateRequest.class);
        SelectedPlace selected = org.mockito.Mockito.mock(SelectedPlace.class);
        given(request.requestId()).willReturn(requestId);
        given(request.places()).willReturn(List.of());
        given(postReader.findIdByRequestId(1L, requestId.toString())).willReturn(Optional.empty());
        given(placeSelectionResolver.resolve(1L, List.of())).willReturn(List.of(selected));
        given(metricsRecorder.recordTransaction(eq("current"), eq("post"), any()))
                .willAnswer(invocation -> ((Supplier<Long>) invocation.getArgument(2)).get());
        given(processor.create(eq(1L), eq(requestId.toString()), any(), eq(List.of(selected)), eq("127.0.0.1")))
                .willReturn(43L);

        Long result = service.create(1L, request, "127.0.0.1");

        assertThat(result).isEqualTo(43L);
        verify(placeSelectionResolver).resolve(1L, List.of());
        verify(processor).create(eq(1L), eq(requestId.toString()), any(), eq(List.of(selected)), eq("127.0.0.1"));
    }
}
