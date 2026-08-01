package cluverse.place.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.place.service.PlaceQueryService;
import cluverse.place.service.response.PlaceContentsResponse;
import cluverse.place.service.response.PlaceDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/places")
@RequiredArgsConstructor
public class PlaceQueryControllerV2 {

    private final PlaceQueryService placeQueryService;

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
