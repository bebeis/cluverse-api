package cluverse.event.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.event.service.CampusEventQueryService;
import cluverse.event.service.request.CampusEventSearchRequest;
import cluverse.event.service.response.CampusEventResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class CampusEventController {

    private final CampusEventQueryService campusEventQueryService;

    @GetMapping
    public ApiResponse<List<CampusEventResponse>> getEvents(@Valid @ModelAttribute CampusEventSearchRequest request) {
        return ApiResponse.ok(campusEventQueryService.getEvents(request));
    }

    @GetMapping("/{eventId}")
    public ApiResponse<CampusEventResponse> getEvent(@PathVariable Long eventId) {
        return ApiResponse.ok(campusEventQueryService.getEvent(eventId));
    }
}
