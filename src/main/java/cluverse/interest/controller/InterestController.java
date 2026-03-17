package cluverse.interest.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.interest.service.InterestService;
import cluverse.interest.service.response.InterestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/interests")
@RequiredArgsConstructor
public class InterestController {

    private final InterestService interestService;

    @GetMapping
    public ApiResponse<List<InterestResponse>> getInterests() {
        return ApiResponse.ok(interestService.getInterests());
    }
}
