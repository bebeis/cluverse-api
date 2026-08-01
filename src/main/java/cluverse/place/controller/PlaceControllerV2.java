package cluverse.place.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.place.service.PlaceQueryService;
import cluverse.place.service.PlaceSearchServiceV2;
import cluverse.place.service.response.PlaceContentsResponse;
import cluverse.place.service.response.PlaceDetailResponse;
import cluverse.place.service.response.PlaceSearchResponseV2;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v2/places")
@RequiredArgsConstructor
public class PlaceControllerV2 {

    private final PlaceSearchServiceV2 placeSearchService;
    private final PlaceQueryService placeQueryService;

    @GetMapping("/search")
    public ApiResponse<PlaceSearchResponseV2> search(
            @Login LoginMember loginMember,
            @RequestParam @NotBlank @Size(max = 100) String query
    ) {
        return ApiResponse.ok(placeSearchService.search(loginMember.memberId(), query));
    }

    @GetMapping("/{placeId}")
    public ApiResponse<PlaceDetailResponse> readDetail(@PathVariable Long placeId) {
        return ApiResponse.ok(placeQueryService.readDetail(placeId));
    }

    @GetMapping("/{placeId}/contents")
    public ApiResponse<PlaceContentsResponse> readContents(
            @PathVariable Long placeId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.ok(placeQueryService.readContents(placeId, cursor, size));
    }
}
