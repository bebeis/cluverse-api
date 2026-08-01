package cluverse.comment.service.request;

import cluverse.place.service.request.PlaceSelectionRequestV1;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CommentWithPlaceCreateRequestV1(
        @Valid @NotNull CommentCreateRequest comment,
        @Valid @NotNull PlaceSelectionRequestV1 place
) {
}
