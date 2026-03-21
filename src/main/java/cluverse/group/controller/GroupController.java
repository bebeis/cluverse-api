package cluverse.group.controller;

import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.common.exception.UnauthorizedException;
import cluverse.group.service.GroupQueryService;
import cluverse.group.service.GroupService;
import cluverse.group.service.request.GroupCreateRequest;
import cluverse.group.service.request.GroupMemberUpdateRequest;
import cluverse.group.service.request.GroupOwnerTransferRequest;
import cluverse.group.service.request.GroupRoleCreateRequest;
import cluverse.group.service.request.GroupRoleUpdateRequest;
import cluverse.group.service.request.GroupSearchRequest;
import cluverse.group.service.request.GroupUpdateRequest;
import cluverse.group.service.response.GroupDetailResponse;
import cluverse.group.service.response.GroupMemberResponse;
import cluverse.group.service.response.GroupPageResponse;
import cluverse.group.service.response.GroupRoleResponse;
import cluverse.group.service.response.MyGroupSummaryResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupQueryService groupQueryService;
    private final GroupService groupService;

    @GetMapping
    public ApiResponse<GroupPageResponse> getGroups(@Login LoginMember loginMember,
                                                    @Valid @ModelAttribute GroupSearchRequest request) {
        return ApiResponse.ok(groupQueryService.getGroups(extractMemberId(loginMember), request));
    }

    @GetMapping("/me")
    public ApiResponse<List<MyGroupSummaryResponse>> getMyGroups(@Login LoginMember loginMember) {
        return ApiResponse.ok(groupQueryService.getMyGroups(requireLoginMember(loginMember).memberId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GroupDetailResponse> createGroup(@Login LoginMember loginMember,
                                                        @RequestBody @Valid GroupCreateRequest request) {
        return ApiResponse.created(groupService.createGroup(loginMember.memberId(), request));
    }

    @GetMapping("/{groupId}")
    public ApiResponse<GroupDetailResponse> getGroup(@Login LoginMember loginMember,
                                                     @PathVariable Long groupId) {
        return ApiResponse.ok(groupQueryService.getGroup(extractMemberId(loginMember), groupId));
    }

    @PutMapping("/{groupId}")
    public ApiResponse<GroupDetailResponse> updateGroup(@Login LoginMember loginMember,
                                                        @PathVariable Long groupId,
                                                        @RequestBody @Valid GroupUpdateRequest request) {
        return ApiResponse.ok(groupService.updateGroup(loginMember.memberId(), groupId, request));
    }

    @DeleteMapping("/{groupId}")
    public ApiResponse<Void> deleteGroup(@Login LoginMember loginMember,
                                         @PathVariable Long groupId) {
        groupService.deleteGroup(loginMember.memberId(), groupId);
        return ApiResponse.ok();
    }

    @GetMapping("/{groupId}/members")
    public ApiResponse<List<GroupMemberResponse>> getMembers(@Login LoginMember loginMember,
                                                             @PathVariable Long groupId) {
        return ApiResponse.ok(groupQueryService.getMembers(loginMember.memberId(), groupId));
    }

    @PatchMapping("/{groupId}/members/{memberId}")
    public ApiResponse<GroupMemberResponse> updateMember(@Login LoginMember loginMember,
                                                         @PathVariable Long groupId,
                                                         @PathVariable Long memberId,
                                                         @RequestBody @Valid GroupMemberUpdateRequest request,
                                                         HttpServletRequest httpRequest) {
        return ApiResponse.ok(
                groupService.updateMember(
                        loginMember.memberId(),
                        groupId,
                        memberId,
                        request,
                        httpRequest.getRemoteAddr()
                )
        );
    }

    @DeleteMapping("/{groupId}/members/me")
    public ApiResponse<Void> leaveGroup(@Login LoginMember loginMember,
                                        @PathVariable Long groupId,
                                        HttpServletRequest httpRequest) {
        groupService.leaveGroup(loginMember.memberId(), groupId, httpRequest.getRemoteAddr());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    public ApiResponse<Void> removeMember(@Login LoginMember loginMember,
                                          @PathVariable Long groupId,
                                          @PathVariable Long memberId,
                                          HttpServletRequest httpRequest) {
        groupService.removeMember(loginMember.memberId(), groupId, memberId, httpRequest.getRemoteAddr());
        return ApiResponse.ok();
    }

    @PostMapping("/{groupId}/owner-transfer")
    public ApiResponse<GroupDetailResponse> transferOwner(@Login LoginMember loginMember,
                                                          @PathVariable Long groupId,
                                                          @RequestBody @Valid GroupOwnerTransferRequest request,
                                                          HttpServletRequest httpRequest) {
        return ApiResponse.ok(
                groupService.transferOwner(
                        loginMember.memberId(),
                        groupId,
                        request,
                        httpRequest.getRemoteAddr()
                )
        );
    }

    @GetMapping("/{groupId}/roles")
    public ApiResponse<List<GroupRoleResponse>> getRoles(@Login LoginMember loginMember,
                                                         @PathVariable Long groupId) {
        return ApiResponse.ok(groupQueryService.getRoles(loginMember.memberId(), groupId));
    }

    @PostMapping("/{groupId}/roles")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GroupRoleResponse> createRole(@Login LoginMember loginMember,
                                                     @PathVariable Long groupId,
                                                     @RequestBody @Valid GroupRoleCreateRequest request) {
        return ApiResponse.created(groupService.createRole(loginMember.memberId(), groupId, request));
    }

    @PutMapping("/{groupId}/roles/{roleId}")
    public ApiResponse<GroupRoleResponse> updateRole(@Login LoginMember loginMember,
                                                     @PathVariable Long groupId,
                                                     @PathVariable Long roleId,
                                                     @RequestBody @Valid GroupRoleUpdateRequest request) {
        return ApiResponse.ok(groupService.updateRole(loginMember.memberId(), groupId, roleId, request));
    }

    @DeleteMapping("/{groupId}/roles/{roleId}")
    public ApiResponse<Void> deleteRole(@Login LoginMember loginMember,
                                        @PathVariable Long groupId,
                                        @PathVariable Long roleId) {
        groupService.deleteRole(loginMember.memberId(), groupId, roleId);
        return ApiResponse.ok();
    }

    private Long extractMemberId(LoginMember loginMember) {
        return loginMember == null ? null : loginMember.memberId();
    }

    private LoginMember requireLoginMember(LoginMember loginMember) {
        if (loginMember == null) {
            throw new UnauthorizedException(AuthExceptionMessage.UNAUTHORIZED.getMessage());
        }
        return loginMember;
    }
}
