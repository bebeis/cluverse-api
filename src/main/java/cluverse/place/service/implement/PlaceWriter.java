package cluverse.place.service.implement;

import cluverse.place.domain.Place;
import cluverse.place.domain.PlaceCandidate;
import cluverse.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional
public class PlaceWriter {

    private final PlaceRepository placeRepository;

    public Place upsert(PlaceCandidate candidate) {
        LocalDateTime synchronizedAt = LocalDateTime.now();
        placeRepository.upsert(
                candidate.provider(), candidate.sourceFingerprint(), candidate.name(), candidate.category().name(),
                candidate.rawCategory(), candidate.address(), candidate.roadAddress(), candidate.latitude(),
                candidate.longitude(), candidate.sourceUrl(), synchronizedAt
        );
        Long placeId = placeRepository.lastInsertedId();
        return placeRepository.getReferenceById(placeId);
    }

    public List<Place> upsertAll(List<PlaceCandidate> candidates) {
        return candidates.stream().map(this::upsert).toList();
    }
}
