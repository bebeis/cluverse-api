package cluverse.calendar.controller;

import cluverse.calendar.service.CalendarEventQueryService;
import cluverse.calendar.service.CalendarEventService;
import cluverse.calendar.service.request.CalendarEventCreateRequest;
import cluverse.calendar.service.request.CalendarEventSearchRequest;
import cluverse.calendar.service.request.CalendarEventUpdateRequest;
import cluverse.calendar.service.request.UpcomingCalendarEventRequest;
import cluverse.calendar.service.response.CalendarEventResponse;
import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/calendar/events")
@RequiredArgsConstructor
public class CalendarEventController {

    private final CalendarEventQueryService calendarEventQueryService;
    private final CalendarEventService calendarEventService;

    @GetMapping
    public ApiResponse<List<CalendarEventResponse>> getEvents(@Login LoginMember loginMember,
                                                              @Valid @ModelAttribute CalendarEventSearchRequest request) {
        return ApiResponse.ok(calendarEventQueryService.getEvents(extractMemberId(loginMember), request));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CalendarEventResponse> createEvent(@Login LoginMember loginMember,
                                                          @RequestBody @Valid CalendarEventCreateRequest request) {
        return ApiResponse.created(calendarEventService.createEvent(extractMemberId(loginMember), request));
    }

    @PutMapping("/{eventId}")
    public ApiResponse<CalendarEventResponse> updateEvent(@Login LoginMember loginMember,
                                                          @PathVariable Long eventId,
                                                          @RequestBody @Valid CalendarEventUpdateRequest request) {
        return ApiResponse.ok(calendarEventService.updateEvent(extractMemberId(loginMember), eventId, request));
    }

    @DeleteMapping("/{eventId}")
    public ApiResponse<Void> deleteEvent(@Login LoginMember loginMember,
                                         @PathVariable Long eventId) {
        calendarEventService.deleteEvent(extractMemberId(loginMember), eventId);
        return ApiResponse.ok();
    }

    @GetMapping("/upcoming")
    public ApiResponse<List<CalendarEventResponse>> getUpcomingEvents(@Login LoginMember loginMember,
                                                                      @Valid @ModelAttribute UpcomingCalendarEventRequest request) {
        return ApiResponse.ok(calendarEventQueryService.getUpcomingEvents(extractMemberId(loginMember), request.sizeOrDefault()));
    }

    private Long extractMemberId(LoginMember loginMember) {
        return loginMember == null ? null : loginMember.memberId();
    }
}
