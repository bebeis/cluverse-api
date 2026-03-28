package cluverse.member.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.member.service.MemberPostQueryService;
import cluverse.member.service.MemberQueryService;
import cluverse.member.service.MemberService;
import cluverse.member.service.MemberUniversityService;
import cluverse.member.service.MemberProfileImageService;
import cluverse.member.service.request.AddInterestRequest;
import cluverse.member.service.request.AddMajorRequest;
import cluverse.member.service.request.MemberNicknameCheckRequest;
import cluverse.member.service.request.MemberNicknameUpdateRequest;
import cluverse.member.service.request.MemberPasswordUpdateRequest;
import cluverse.member.service.request.MemberPostPageRequest;
import cluverse.member.service.request.MemberProfileImagePresignedUrlRequest;
import cluverse.member.service.request.MemberUniversityUpdateRequest;
import cluverse.member.service.request.UpdateProfileRequest;
import cluverse.member.service.response.BlockedMemberResponse;
import cluverse.member.service.response.MemberFollowResponse;
import cluverse.member.service.response.MemberInterestResponse;
import cluverse.member.service.response.MemberMajorResponse;
import cluverse.member.service.response.MemberNicknameAvailabilityResponse;
import cluverse.member.service.response.MemberProfileResponse;
import cluverse.member.service.response.MemberProfileImagePresignedUrlResponse;
import cluverse.post.service.response.PostPageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberQueryService memberQueryService;
    private final MemberService memberService;
    private final MemberUniversityService memberUniversityService;
    private final MemberPostQueryService memberPostQueryService;
    private final MemberProfileImageService memberProfileImageService;

    @GetMapping("/me/profile")
    public ApiResponse<MemberProfileResponse> getMyProfile(@Login LoginMember loginMember) {
        return ApiResponse.ok(memberQueryService.getProfile(loginMember.memberId(), loginMember.memberId()));
    }

    @GetMapping("/{memberId}/profile")
    public ApiResponse<MemberProfileResponse> getProfile(@Login LoginMember loginMember,
                                                         @PathVariable Long memberId) {
        return ApiResponse.ok(memberQueryService.getProfile(loginMember.memberId(), memberId));
    }

    @GetMapping("/nickname/availability")
    public ApiResponse<MemberNicknameAvailabilityResponse> checkNicknameAvailability(
            @Valid @ModelAttribute MemberNicknameCheckRequest request
    ) {
        return ApiResponse.ok(memberQueryService.checkNicknameAvailability(request.nickname()));
    }

    @PutMapping("/me/profile")
    public ApiResponse<MemberProfileResponse> updateProfile(@Login LoginMember loginMember,
                                                             @RequestBody @Valid UpdateProfileRequest request) {
        return ApiResponse.ok(memberService.updateProfile(loginMember.memberId(), request));
    }

    @PatchMapping("/me/nickname")
    public ApiResponse<MemberProfileResponse> updateNickname(@Login LoginMember loginMember,
                                                             @RequestBody @Valid MemberNicknameUpdateRequest request) {
        return ApiResponse.ok(memberService.updateNickname(loginMember.memberId(), request));
    }

    @PutMapping("/me/university")
    public ApiResponse<MemberProfileResponse> updateUniversity(@Login LoginMember loginMember,
                                                               @RequestBody @Valid MemberUniversityUpdateRequest request) {
        return ApiResponse.ok(memberUniversityService.updateUniversity(loginMember.memberId(), request));
    }

    @GetMapping("/me/posts")
    public ApiResponse<PostPageResponse> getMyPosts(@Login LoginMember loginMember,
                                                    @Valid @ModelAttribute MemberPostPageRequest request) {
        return ApiResponse.ok(memberPostQueryService.getMyPosts(loginMember.memberId(), request));
    }

    @PatchMapping("/me/password")
    public ApiResponse<Void> updatePassword(@Login LoginMember loginMember,
                                            @RequestBody @Valid MemberPasswordUpdateRequest request) {
        memberService.updatePassword(loginMember.memberId(), request);
        return ApiResponse.ok();
    }

    @PostMapping("/me/profile-image/presigned-url")
    public ApiResponse<MemberProfileImagePresignedUrlResponse> createProfileImagePresignedUrl(
            @Login LoginMember loginMember,
            @RequestBody @Valid MemberProfileImagePresignedUrlRequest request
    ) {
        return ApiResponse.ok(memberProfileImageService.createPresignedUrl(loginMember.memberId(), request));
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMember(@Login LoginMember loginMember) {
        memberService.deleteMember(loginMember.memberId());
        return ApiResponse.ok();
    }

    @GetMapping("/me/majors")
    public ApiResponse<List<MemberMajorResponse>> getMyMajors(@Login LoginMember loginMember) {
        return ApiResponse.ok(memberQueryService.getMajors(loginMember.memberId()));
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
        return ApiResponse.ok(memberQueryService.getInterests(loginMember.memberId()));
    }

    @GetMapping("/me/blocks")
    public ApiResponse<List<BlockedMemberResponse>> getMyBlocks(@Login LoginMember loginMember) {
        return ApiResponse.ok(memberQueryService.getBlockedMembers(loginMember.memberId()));
    }

    @GetMapping("/{memberId}/followers")
    public ApiResponse<List<MemberFollowResponse>> getFollowers(@PathVariable Long memberId) {
        return ApiResponse.ok(memberQueryService.getFollowers(memberId));
    }

    @GetMapping("/{memberId}/following")
    public ApiResponse<List<MemberFollowResponse>> getFollowings(@PathVariable Long memberId) {
        return ApiResponse.ok(memberQueryService.getFollowings(memberId));
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
