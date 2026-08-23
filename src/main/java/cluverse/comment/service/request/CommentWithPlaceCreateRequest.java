package cluverse.comment.service.request;

import cluverse.place.service.request.PlaceSelectionRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CommentWithPlaceCreateRequest(
        @NotNull UUID requestId,
        @Valid @NotNull CommentCreateRequest comment,
        @Valid @NotNull PlaceSelectionRequest place
) {
}
