package cluverse.post.service.implement;

import cluverse.place.domain.Place;
import cluverse.place.domain.PlaceCandidate;
import cluverse.place.domain.PlaceCategory;
import cluverse.place.domain.PlaceProvider;
import cluverse.place.domain.ResolvedPlaceAttachment;
import cluverse.place.domain.SelectedPlace;
import cluverse.place.service.implement.LocalPlaceAttachmentResolver;
import cluverse.place.service.implement.PlaceWriter;
import cluverse.post.domain.Post;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostPlaceCompletionProcessorTest {

    @Mock
    private PostAccessReader postAccessReader;

    @Mock
    private LocalPlaceAttachmentResolver attachmentResolver;

    @Mock
    private PlaceWriter placeWriter;

    @Mock
    private Post post;

    @Mock
    private Place place;

    @InjectMocks
    private PostPlaceCompletionProcessor processor;

    @Test
    void 장소_UPSERT로_영속성_컨텍스트가_비워진_뒤_게시글을_다시_조회해_연결한다() {
        // given
        PlaceCandidate candidate = new PlaceCandidate(
                PlaceProvider.NAVER,
                "fingerprint",
                "장소",
                PlaceCategory.CAFE,
                "카페",
                "주소",
                "도로명 주소",
                BigDecimal.ONE,
                BigDecimal.ONE,
                "https://example.com/place"
        );
        SelectedPlace selected = new SelectedPlace(candidate, false);
        ResolvedPlaceAttachment attachment = new ResolvedPlaceAttachment(candidate, 1L, null, false);

        given(attachmentResolver.resolve(1L, List.of(selected))).willReturn(List.of(attachment));
        given(placeWriter.upsertAll(List.of(candidate))).willReturn(List.of(place));
        given(postAccessReader.readWithPlacesOrThrow(10L)).willReturn(post);
        given(place.getId()).willReturn(20L);

        // when
        processor.complete(1L, 10L, List.of(selected));

        // then
        InOrder order = inOrder(placeWriter, postAccessReader);
        order.verify(placeWriter).upsertAll(List.of(candidate));
        order.verify(postAccessReader).readWithPlacesOrThrow(10L);
        verify(post).addPlace(20L, 0, 1L, null, false);
    }
}
