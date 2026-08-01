package cluverse.place.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.place.client.PlaceSearchClient;
import cluverse.place.domain.PlaceCandidate;
import cluverse.place.domain.SelectedPlace;
import cluverse.place.exception.PlaceExceptionMessage;
import cluverse.place.service.request.PlaceSelectionRequestV1;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class V1PlaceSelectionResolver {

    private final PlaceSearchClient placeSearchClient;
    private final PlaceCandidateFactory placeCandidateFactory;

    public List<SelectedPlace> resolve(List<PlaceSelectionRequestV1> selections) {
        Map<String, List<PlaceCandidate>> candidatesByQuery = new LinkedHashMap<>();
        List<SelectedPlace> resolved = selections.stream()
                .map(selection -> resolve(selection, candidatesByQuery))
                .toList();
        validateNoDuplicate(resolved);
        return resolved;
    }

    private SelectedPlace resolve(PlaceSelectionRequestV1 selection,
                                  Map<String, List<PlaceCandidate>> candidatesByQuery) {
        List<PlaceCandidate> candidates = candidatesByQuery.computeIfAbsent(
                selection.query(),
                query -> placeSearchClient.search(query).stream().map(placeCandidateFactory::create).toList()
        );
        PlaceCandidate selected = candidates.stream()
                .filter(candidate -> candidate.sourceFingerprint().equals(selection.sourceFingerprint()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        PlaceExceptionMessage.SELECTED_PLACE_NOT_FOUND.getMessage()));
        return new SelectedPlace(selected, selection.recommended());
    }

    private void validateNoDuplicate(List<SelectedPlace> places) {
        Set<String> fingerprints = new HashSet<>();
        boolean duplicated = places.stream()
                .map(place -> place.candidate().sourceFingerprint())
                .anyMatch(fingerprint -> !fingerprints.add(fingerprint));
        if (duplicated) {
            throw new BadRequestException(PlaceExceptionMessage.DUPLICATED_PLACE_ATTACHMENT.getMessage());
        }
    }
}
