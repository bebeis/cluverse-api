package cluverse.place.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.place.domain.SelectedPlace;
import cluverse.place.exception.PlaceExceptionMessage;
import cluverse.place.service.request.PlaceSelectionRequestV2;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class V2PlaceSelectionResolver {

    private final PlaceSelectionTokenManager tokenManager;

    public List<SelectedPlace> resolve(Long memberId, List<PlaceSelectionRequestV2> selections) {
        List<SelectedPlace> resolved = selections.stream()
                .map(selection -> new SelectedPlace(
                        tokenManager.verify(memberId, selection.selectionToken()).candidate(),
                        selection.recommended()
                ))
                .toList();
        validateNoDuplicate(resolved);
        return resolved;
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
