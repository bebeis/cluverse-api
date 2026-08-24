package cluverse.post.service.implement;

import cluverse.place.domain.PlaceCandidate;
import cluverse.place.domain.SelectedPlace;
import cluverse.place.service.implement.LocalMapMetricsRecorder;
import cluverse.place.service.implement.ExternalPlaceVerificationResolver;
import cluverse.place.service.request.PlaceVerificationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AsyncPostPlaceVerificationHandlerTest {

    @Mock
    private ExternalPlaceVerificationResolver placeVerificationResolver;
    @Mock
    private PostPlaceCompletionProcessor completionProcessor;
    @Mock
    private LocalMapMetricsRecorder metricsRecorder;
    @Mock
    private PostPlaceVerificationWriter verificationWriter;
    @InjectMocks
    private AsyncPostPlaceVerificationHandler handler;

    @Test
    void 외부_검증에_성공하면_별도_완료_트랜잭션에_장소를_전달한다() {
        PlaceCandidate candidate = org.mockito.Mockito.mock(PlaceCandidate.class);
        SelectedPlace pending = new SelectedPlace(candidate, true);
        SelectedPlace verified = org.mockito.Mockito.mock(SelectedPlace.class);
        given(candidate.name()).willReturn("연세대 카페");
        given(candidate.sourceFingerprint()).willReturn("fingerprint");
        given(placeVerificationResolver.resolve(List.of(
                new PlaceVerificationRequest("연세대 카페", "fingerprint", true)
        ))).willReturn(List.of(verified));
        given(metricsRecorder.recordAsync(org.mockito.ArgumentMatchers.eq("provider"),
                org.mockito.ArgumentMatchers.<Supplier<List<SelectedPlace>>>any()))
                .willAnswer(invocation -> ((Supplier<List<SelectedPlace>>) invocation.getArgument(1)).get());
        org.mockito.Mockito.doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(1)).run();
            return null;
        }).when(metricsRecorder).recordAsync(org.mockito.ArgumentMatchers.eq("completion"),
                org.mockito.ArgumentMatchers.any(Runnable.class));

        handler.verify(new PostPlaceVerificationRequested(1L, 10L, List.of(pending)));

        verify(completionProcessor).complete(1L, 10L, List.of(verified));
    }

    @Test
    void 외부_검증이_실패해도_게시글_트랜잭션을_되돌리지_않고_장소_완료를_실행하지_않는다() {
        PlaceCandidate candidate = org.mockito.Mockito.mock(PlaceCandidate.class);
        SelectedPlace pending = new SelectedPlace(candidate, true);
        given(candidate.name()).willReturn("연세대 카페");
        given(candidate.sourceFingerprint()).willReturn("fingerprint");
        given(metricsRecorder.recordAsync(org.mockito.ArgumentMatchers.eq("provider"),
                org.mockito.ArgumentMatchers.<Supplier<List<SelectedPlace>>>any()))
                .willAnswer(invocation -> ((Supplier<List<SelectedPlace>>) invocation.getArgument(1)).get());
        given(placeVerificationResolver.resolve(anyList())).willThrow(new IllegalStateException("provider timeout"));

        handler.verify(new PostPlaceVerificationRequested(1L, 10L, List.of(pending)));

        verify(completionProcessor, never()).complete(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(), anyList());
        verify(verificationWriter).fail(10L, "provider timeout");
    }
}
