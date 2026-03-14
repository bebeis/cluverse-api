package cluverse.university.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.university.service.UniversityService;
import cluverse.university.service.request.UniversityCreateRequest;
import cluverse.university.service.request.UniversitySearchRequest;
import cluverse.university.service.request.UniversityUpdateRequest;
import cluverse.university.service.response.UniversityDetailResponse;
import cluverse.university.service.response.UniversitySummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/universities")
@RequiredArgsConstructor
public class UniversityController {

    private final UniversityService universityService;

    @GetMapping
    public ApiResponse<List<UniversitySummaryResponse>> searchUniversities(
            @Valid @ModelAttribute UniversitySearchRequest request
    ) {
        return ApiResponse.ok(universityService.searchUniversities(request));
    }

    @GetMapping("/{universityId}")
    public ApiResponse<UniversityDetailResponse> getUniversity(@PathVariable Long universityId) {
        return ApiResponse.ok(universityService.getUniversity(universityId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UniversityDetailResponse> createUniversity(
            @Login LoginMember loginMember,
            @RequestBody @Valid UniversityCreateRequest request
    ) {
        return ApiResponse.created(universityService.createUniversity(loginMember.memberId(), request));
    }

    @PutMapping("/{universityId}")
    public ApiResponse<UniversityDetailResponse> updateUniversity(
            @Login LoginMember loginMember,
            @PathVariable Long universityId,
            @RequestBody @Valid UniversityUpdateRequest request
    ) {
        return ApiResponse.ok(universityService.updateUniversity(loginMember.memberId(), universityId, request));
    }
}
