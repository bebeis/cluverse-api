package cluverse.group.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class GroupService {

    @Transactional(readOnly = true)
    public GroupPageResponse getGroups(Long memberId, GroupSearchRequest request) {
        throw unsupported();
    }

    @Transactional(readOnly = true)
    public List<MyGroupSummaryResponse> getMyGroups(Long memberId) {
        throw unsupported();
    }

    public GroupDetailResponse createGroup(Long memberId, GroupCreateRequest request) {
        throw unsupported();
    }

    @Transactional(readOnly = true)
    public GroupDetailResponse getGroup(Long memberId, Long groupId) {
        throw unsupported();
    }

    public GroupDetailResponse updateGroup(Long memberId, Long groupId, GroupUpdateRequest request) {
        throw unsupported();
    }

    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getMembers(Long memberId, Long groupId) {
        throw unsupported();
    }

    public GroupMemberResponse updateMember(Long memberId,
                                            Long groupId,
                                            Long targetMemberId,
                                            GroupMemberUpdateRequest request,
                                            String clientIp) {
        throw unsupported();
    }

    public void leaveGroup(Long memberId, Long groupId, String clientIp) {
        throw unsupported();
    }

    public void removeMember(Long memberId, Long groupId, Long targetMemberId, String clientIp) {
        throw unsupported();
    }

    public GroupDetailResponse transferOwner(Long memberId,
                                             Long groupId,
                                             GroupOwnerTransferRequest request,
                                             String clientIp) {
        throw unsupported();
    }

    @Transactional(readOnly = true)
    public List<GroupRoleResponse> getRoles(Long memberId, Long groupId) {
        throw unsupported();
    }

    public GroupRoleResponse createRole(Long memberId, Long groupId, GroupRoleCreateRequest request) {
        throw unsupported();
    }

    public GroupRoleResponse updateRole(Long memberId, Long groupId, Long roleId, GroupRoleUpdateRequest request) {
        throw unsupported();
    }

    public void deleteRole(Long memberId, Long groupId, Long roleId) {
        throw unsupported();
    }

    private UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Group API contract only. Service implementation is pending.");
    }
}
