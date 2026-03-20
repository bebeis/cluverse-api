package cluverse.member.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.member.service.MemberUniversityService;
import cluverse.member.service.request.AddInterestRequest;
import cluverse.member.service.request.AddMajorRequest;
import cluverse.member.service.request.MemberUniversityUpdateRequest;
import cluverse.member.service.request.UpdateProfileRequest;
import cluverse.member.service.MemberService;
import cluverse.member.service.response.BlockedMemberResponse;
import cluverse.member.service.response.MemberInterestResponse;
import cluverse.member.service.response.MemberMajorResponse;
import cluverse.member.service.response.MemberProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberUniversityService memberUniversityService;

    @GetMapping("/me/profile")
    public ApiResponse<MemberProfileResponse> getMyProfile(@Login LoginMember loginMember) {
        return ApiResponse.ok(memberService.getProfile(loginMember.memberId(), loginMember.memberId()));
    }

    @GetMapping("/{memberId}/profile")
    public ApiResponse<MemberProfileResponse> getProfile(@Login LoginMember loginMember,
                                                         @PathVariable Long memberId) {
        return ApiResponse.ok(memberService.getProfile(loginMember.memberId(), memberId));
    }

    @PutMapping("/me/profile")
    public ApiResponse<MemberProfileResponse> updateProfile(@Login LoginMember loginMember,
                                                             @RequestBody @Valid UpdateProfileRequest request) {
        return ApiResponse.ok(memberService.updateProfile(loginMember.memberId(), request));
    }

    @PutMapping("/me/university")
    public ApiResponse<MemberProfileResponse> updateUniversity(@Login LoginMember loginMember,
                                                               @RequestBody @Valid MemberUniversityUpdateRequest request) {
        return ApiResponse.ok(memberUniversityService.updateUniversity(loginMember.memberId(), request));
    }

    @GetMapping("/me/majors")
    public ApiResponse<List<MemberMajorResponse>> getMyMajors(@Login LoginMember loginMember) {
        return ApiResponse.ok(memberService.getMajors(loginMember.memberId()));
    }

    @PostMapping("/me/majors")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberMajorResponse> addMajor(@Login LoginMember loginMember,
                                                      @RequestBody @Valid AddMajorRequest request) {
        return ApiResponse.created(memberService.addMajor(loginMember.memberId(), request));
    }

    @DeleteMapping("/me/majors/{majorId}")
    public ApiResponse<Void> removeMajor(@Login LoginMember loginMember,
                                          @PathVariable Long majorId) {
        memberService.removeMajor(loginMember.memberId(), majorId);
        return ApiResponse.ok();
    }

    @GetMapping("/me/interests")
    public ApiResponse<List<MemberInterestResponse>> getMyInterests(@Login LoginMember loginMember) {
        return ApiResponse.ok(memberService.getInterests(loginMember.memberId()));
    }

    @GetMapping("/me/blocks")
    public ApiResponse<List<BlockedMemberResponse>> getMyBlocks(@Login LoginMember loginMember) {
        return ApiResponse.ok(memberService.getBlockedMembers(loginMember.memberId()));
    }

    @PostMapping("/me/interests")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberInterestResponse> addInterest(@Login LoginMember loginMember,
                                                            @RequestBody @Valid AddInterestRequest request) {
        return ApiResponse.created(memberService.addInterest(loginMember.memberId(), request));
    }

    @DeleteMapping("/me/interests/{interestId}")
    public ApiResponse<Void> removeInterest(@Login LoginMember loginMember,
                                             @PathVariable Long interestId) {
        memberService.removeInterest(loginMember.memberId(), interestId);
        return ApiResponse.ok();
    }

    @PostMapping("/{memberId}/follow")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> follow(@Login LoginMember loginMember,
                                    @PathVariable Long memberId) {
        memberService.follow(loginMember.memberId(), memberId);
        return ApiResponse.created(null);
    }

    @DeleteMapping("/{memberId}/follow")
    public ApiResponse<Void> unfollow(@Login LoginMember loginMember,
                                       @PathVariable Long memberId) {
        memberService.unfollow(loginMember.memberId(), memberId);
        return ApiResponse.ok();
    }

    @PostMapping("/{memberId}/block")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> block(@Login LoginMember loginMember,
                                    @PathVariable Long memberId) {
        memberService.block(loginMember.memberId(), memberId);
        return ApiResponse.created(null);
    }

    @DeleteMapping("/{memberId}/block")
    public ApiResponse<Void> unblock(@Login LoginMember loginMember,
                                      @PathVariable Long memberId) {
        memberService.unblock(loginMember.memberId(), memberId);
        return ApiResponse.ok();
    }
}
