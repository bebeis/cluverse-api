package cluverse.place.service.implement;

import cluverse.common.exception.NotFoundException;
import cluverse.place.domain.Place;
import cluverse.place.exception.PlaceExceptionMessage;
import cluverse.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceReader {

    private final PlaceRepository placeRepository;

    public Place readOrThrow(Long placeId) {
        return placeRepository.findById(placeId)
                .orElseThrow(() -> new NotFoundException(PlaceExceptionMessage.PLACE_NOT_FOUND.getMessage()));
    }
}
