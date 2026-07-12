package cluverse.group.service;

import cluverse.group.service.implement.GroupProcessor;
import cluverse.group.service.request.GroupCreateRequest;
import cluverse.group.service.request.GroupMemberUpdateRequest;
import cluverse.group.service.request.GroupOwnerTransferRequest;
import cluverse.group.service.request.GroupRoleCreateRequest;
import cluverse.group.service.request.GroupRoleUpdateRequest;
import cluverse.group.service.request.GroupUpdateRequest;
import cluverse.group.service.response.GroupDetailResponse;
import cluverse.group.service.response.GroupMemberResponse;
import cluverse.group.service.response.GroupRoleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupProcessor groupProcessor;

    public GroupDetailResponse createGroup(Long memberId, GroupCreateRequest request) {
        return groupProcessor.createGroup(memberId, request);
    }

    public GroupDetailResponse updateGroup(Long memberId, Long groupId, GroupUpdateRequest request) {
        return groupProcessor.updateGroup(memberId, groupId, request);
    }

    public GroupMemberResponse updateMember(Long memberId,
                                            Long groupId,
                                            Long targetMemberId,
                                            GroupMemberUpdateRequest request,
                                            String clientIp) {
        return groupProcessor.updateMember(memberId, groupId, targetMemberId, request);
    }

    public void leaveGroup(Long memberId, Long groupId, String clientIp) {
        groupProcessor.leaveGroup(memberId, groupId);
    }

    public void removeMember(Long memberId, Long groupId, Long targetMemberId, String clientIp) {
        groupProcessor.removeMember(memberId, groupId, targetMemberId);
    }

    public GroupDetailResponse transferOwner(Long memberId,
                                             Long groupId,
                                             GroupOwnerTransferRequest request,
                                             String clientIp) {
        return groupProcessor.transferOwner(memberId, groupId, request);
    }

    public GroupRoleResponse createRole(Long memberId, Long groupId, GroupRoleCreateRequest request) {
        return groupProcessor.createRole(memberId, groupId, request);
    }

    public GroupRoleResponse updateRole(Long memberId, Long groupId, Long roleId, GroupRoleUpdateRequest request) {
        return groupProcessor.updateRole(memberId, groupId, roleId, request);
    }

    public void deleteRole(Long memberId, Long groupId, Long roleId) {
        groupProcessor.deleteRole(memberId, groupId, roleId);
    }

    public void deleteGroup(Long memberId, Long groupId) {
        groupProcessor.deleteGroup(memberId, groupId);
    }
}
