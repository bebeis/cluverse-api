package cluverse.event.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.event.service.CampusEventQueryService;
import cluverse.event.service.request.CampusEventSearchRequest;
import cluverse.event.service.response.CampusEventResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class CampusEventController {

    private final CampusEventQueryService campusEventQueryService;

    @GetMapping
    public ApiResponse<List<CampusEventResponse>> getEvents(@ModelAttribute CampusEventSearchRequest request) {
        return ApiResponse.ok(campusEventQueryService.getEvents(request));
    }

    @GetMapping("/{eventId}")
    public ApiResponse<CampusEventResponse> getEvent(@PathVariable Long eventId) {
        return ApiResponse.ok(campusEventQueryService.getEvent(eventId));
    }
}
