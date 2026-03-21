package cluverse.major.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.major.service.MajorQueryService;
import cluverse.major.service.response.MajorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/majors")
@RequiredArgsConstructor
public class MajorController {

    private final MajorQueryService majorQueryService;

    @GetMapping
    public ApiResponse<List<MajorResponse>> getMajors(@RequestParam(required = false) Long parentMajorId) {
        return ApiResponse.ok(majorQueryService.getMajors(parentMajorId));
    }
}
