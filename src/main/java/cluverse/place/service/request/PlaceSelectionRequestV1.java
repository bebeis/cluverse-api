package cluverse.place.service.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaceSelectionRequestV1(
        @NotBlank @Size(max = 100) String query,
        @NotBlank @Size(max = 64) String sourceFingerprint,
        boolean recommended
) {
}
