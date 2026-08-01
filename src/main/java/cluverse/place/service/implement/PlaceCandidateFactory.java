package cluverse.place.service.implement;

import cluverse.place.domain.PlaceCandidate;
import cluverse.place.domain.PlaceCategory;
import cluverse.place.domain.PlaceSourceCandidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlaceCandidateFactory {

    private final PlaceFingerprintGenerator fingerprintGenerator;

    public PlaceCandidate create(PlaceSourceCandidate source) {
        return new PlaceCandidate(
                source.provider(),
                fingerprintGenerator.generate(source),
                source.name(),
                PlaceCategory.from(source.rawCategory()),
                source.rawCategory(),
                source.address(),
                source.roadAddress(),
                source.latitude(),
                source.longitude(),
                source.sourceUrl()
        );
    }
}
