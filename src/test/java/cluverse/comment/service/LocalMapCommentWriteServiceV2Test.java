package cluverse.comment.service;

import cluverse.comment.service.implement.CommentReader;
import cluverse.comment.service.implement.LocalMapCommentWriteProcessorV2;
import cluverse.comment.service.request.CommentWithPlaceCreateRequestV2;
import cluverse.place.service.implement.LocalMapMetricsRecorder;
import cluverse.place.service.implement.V2PlaceSelectionResolver;
import cluverse.popularity.service.implement.PopularityPromotionInvoker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocalMapCommentWriteServiceV2Test {

    @Mock
    private CommentReader commentReader;
    @Mock
    private V2PlaceSelectionResolver placeSelectionResolver;
    @Mock
    private LocalMapCommentWriteProcessorV2 processor;
    @Mock
    private LocalMapMetricsRecorder metricsRecorder;
    @Mock
    private PopularityPromotionInvoker popularityPromotionInvoker;
    @InjectMocks
    private LocalMapCommentWriteServiceV2 service;

    @Test
    void 같은_requestId가_이미_처리됐으면_장소와_댓글을_다시_처리하지_않는다() {
        UUID requestId = UUID.randomUUID();
        CommentWithPlaceCreateRequestV2 request = org.mockito.Mockito.mock(CommentWithPlaceCreateRequestV2.class);
        given(request.requestId()).willReturn(requestId);
        given(commentReader.findIdByRequestId(1L, requestId.toString())).willReturn(Optional.of(42L));

        Long result = service.create(1L, 10L, request, "127.0.0.1");

        assertThat(result).isEqualTo(42L);
        verify(placeSelectionResolver, never()).resolve(any(), any());
        verify(processor, never()).create(any(), any(), any(), any(), any(), any());
        verify(popularityPromotionInvoker, never()).tryEvaluate(any(), any());
    }
}
