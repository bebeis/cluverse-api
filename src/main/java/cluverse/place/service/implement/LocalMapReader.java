package cluverse.place.service.implement;

import cluverse.place.domain.PlaceCategory;
import cluverse.place.repository.PlaceQueryRepository;
import cluverse.place.repository.dto.LocalMapMarkerQueryResult;
import cluverse.place.repository.dto.PlaceContentQueryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocalMapReader {

    private final PlaceQueryRepository placeQueryRepository;

    public List<LocalMapMarkerQueryResult> readMarkers(
            Long universityId,
            Long campusId,
            PlaceCategory category
    ) {
        return placeQueryRepository.findMarkers(universityId, campusId, category);
    }

    public long countRecommendations(Long placeId) {
        return placeQueryRepository.countRecommendations(placeId);
    }

    public List<PlaceContentQueryResult> readContents(
            Long placeId,
            LocalDateTime cursorCreatedAt,
            String cursorContentType,
            Long cursorContentId,
            int limit
    ) {
        return placeQueryRepository.findContents(
                placeId, cursorCreatedAt, cursorContentType, cursorContentId, limit);
    }
}
