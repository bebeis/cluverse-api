package cluverse.place.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.place.domain.PlaceCategory;
import cluverse.place.service.LocalMapQueryService;
import cluverse.place.service.response.LocalMapResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/local-maps")
@RequiredArgsConstructor
public class LocalMapController {

    private final LocalMapQueryService localMapQueryService;

    @GetMapping("/universities/{universityId}")
    public ApiResponse<LocalMapResponse> read(
            @PathVariable Long universityId,
            @RequestParam(required = false) Long campusId,
            @RequestParam(required = false) PlaceCategory category
    ) {
        return ApiResponse.ok(localMapQueryService.read(universityId, campusId, category));
    }
}
