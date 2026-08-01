package cluverse.post.service.request;

import cluverse.place.service.request.PlaceSelectionRequestV2;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record PostWithPlacesCreateRequestV2(
        @NotNull UUID requestId,
        @Valid @NotNull PostCreateRequest post,
        @Size(max = 5, message = "게시글에는 장소를 최대 5개까지 첨부할 수 있습니다.")
        List<@Valid PlaceSelectionRequestV2> places
) {
    public PostWithPlacesCreateRequestV2 {
        places = places == null ? List.of() : List.copyOf(places);
    }
}
