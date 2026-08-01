package cluverse.place.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.place.service.PlaceSearchServiceV1;
import cluverse.place.service.implement.LocalMapExperimentAuthorizer;
import cluverse.place.service.response.PlaceSearchResponseV1;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class PlaceControllerV1 {

    private final PlaceSearchServiceV1 placeSearchService;
    private final LocalMapExperimentAuthorizer experimentAuthorizer;

    @GetMapping("/search")
    public ApiResponse<PlaceSearchResponseV1> search(
            @RequestHeader(value = "X-Benchmark-Token", required = false) String benchmarkToken,
            @RequestParam @NotBlank @Size(max = 100) String query
    ) {
        experimentAuthorizer.authorize(benchmarkToken);
        return ApiResponse.ok(placeSearchService.search(query));
    }
}
