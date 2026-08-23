package cluverse.place.service.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaceSelectionRequest(
        @NotBlank @Size(max = 8192) String selectionToken,
        boolean recommended
) {
}
