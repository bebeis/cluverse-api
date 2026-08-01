package cluverse.post.service.request;

import cluverse.place.service.request.PlaceSelectionRequestV1;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostWithPlacesCreateRequestV1(
        @Valid @NotNull PostCreateRequest post,
        @Size(max = 5, message = "게시글에는 장소를 최대 5개까지 첨부할 수 있습니다.")
        List<@Valid PlaceSelectionRequestV1> places
) {
    public PostWithPlacesCreateRequestV1 {
        places = places == null ? List.of() : List.copyOf(places);
    }
}
