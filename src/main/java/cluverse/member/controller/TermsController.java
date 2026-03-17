package cluverse.member.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.member.service.TermsService;
import cluverse.member.service.response.TermsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/terms")
@RequiredArgsConstructor
public class TermsController {

    private final TermsService termsService;

    @GetMapping
    public ApiResponse<List<TermsResponse>> getTerms() {
        return ApiResponse.ok(termsService.getTerms());
    }
}
