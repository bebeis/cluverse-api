package cluverse.comment.service.request;

import cluverse.place.service.request.PlaceSelectionRequestV2;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CommentWithPlaceCreateRequestV2(
        @NotNull UUID requestId,
        @Valid @NotNull CommentCreateRequest comment,
        @Valid @NotNull PlaceSelectionRequestV2 place
) {
}
