package cluverse.group.domain;

import cluverse.common.exception.BadRequestException;
import cluverse.common.entity.BaseTimeEntity;
import cluverse.group.exception.GroupExceptionMessage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity(name = "StudyGroup")
@Table(name = "`group`")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Group extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private GroupActivityType activityType;

    private String region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupStatus status;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "max_members")
    private Integer maxMembers;

    @Column(name = "member_count", nullable = false)
    private int memberCount;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupInterest> interests = new ArrayList<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<GroupRole> roles = new ArrayList<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("joinedAt ASC")
    private List<GroupMember> members = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private Group(Long boardId,
                  String name,
                  String description,
                  String coverImageUrl,
                  GroupCategory category,
                  GroupActivityType activityType,
                  String region,
                  GroupVisibility visibility,
                  Long ownerId,
                  Integer maxMembers) {
        this.boardId = boardId;
        this.name = name;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.category = category;
        this.activityType = activityType;
        this.region = region;
        this.visibility = visibility;
        this.status = GroupStatus.ACTIVE;
        this.ownerId = ownerId;
        this.maxMembers = maxMembers;
        this.memberCount = 0;
    }

    public static Group create(Long boardId,
                               String name,
                               String description,
                               String coverImageUrl,
                               GroupCategory category,
                               GroupActivityType activityType,
                               String region,
                               GroupVisibility visibility,
                               Long ownerId,
                               Integer maxMembers,
                               List<Long> interestIds) {
        Group group = Group.builder()
                .boardId(boardId)
                .name(name)
                .description(description)
                .coverImageUrl(coverImageUrl)
                .category(category)
                .activityType(activityType)
                .region(region)
                .visibility(visibility)
                .ownerId(ownerId)
                .maxMembers(maxMembers)
                .build();

        group.replaceInterests(interestIds);
        group.addOwner(ownerId);
        return group;
    }

    public void update(String name,
                       String description,
                       String coverImageUrl,
                       GroupCategory category,
                       GroupActivityType activityType,
                       String region,
                       GroupVisibility visibility,
                       Integer maxMembers,
                       List<Long> interestIds) {
        validateMaxMembers(maxMembers);
        this.name = name;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.category = category;
        this.activityType = activityType;
        this.region = region;
        this.visibility = visibility;
        this.maxMembers = maxMembers;
        replaceInterests(interestIds);
    }

    public GroupRole addRole(String title, int displayOrder) {
        validateRoleTitleUnique(title, null);
        GroupRole role = GroupRole.create(this, title, displayOrder);
        this.roles.add(role);
        return role;
    }

    public void updateRole(Long roleId, String title, int displayOrder) {
        GroupRole role = findRole(roleId);
        validateRoleTitleUnique(title, roleId);
        role.update(title, displayOrder);
    }

    public void deleteRole(Long roleId) {
        GroupRole role = findRole(roleId);
        members.stream()
                .filter(member -> roleId.equals(member.getCustomTitleId()))
                .forEach(GroupMember::clearCustomTitle);
        roles.remove(role);
    }

    public void addMember(Long memberId) {
        validateMemberAbsent(memberId);
        validateMemberCapacity();
        members.add(GroupMember.member(this, memberId));
        memberCount++;
    }

    public void updateMember(Long targetMemberId, GroupMemberRole role, Long customTitleId) {
        GroupMember member = findMember(targetMemberId);
        validateCustomTitle(customTitleId);
        member.update(role, customTitleId);
        if (role == GroupMemberRole.OWNER) {
            transferOwner(targetMemberId);
        }
    }

    public void removeMember(Long targetMemberId) {
        GroupMember member = findMember(targetMemberId);
        if (member.isOwner()) {
            throw new BadRequestException(GroupExceptionMessage.GROUP_OWNER_TRANSFER_REQUIRED.getMessage());
        }
        members.remove(member);
        memberCount--;
    }

    public void leave(Long memberId) {
        GroupMember member = findMember(memberId);
        if (member.isOwner()) {
            throw new BadRequestException(GroupExceptionMessage.GROUP_OWNER_TRANSFER_REQUIRED.getMessage());
        }
        members.remove(member);
        memberCount--;
    }

    public void transferOwner(Long newOwnerMemberId) {
        GroupMember newOwner = findMember(newOwnerMemberId);
        GroupMember previousOwner = findMember(ownerId);
        previousOwner.changeRole(GroupMemberRole.ADMIN);
        newOwner.assignOwner();
        this.ownerId = newOwnerMemberId;
    }

    public boolean isOwner(Long memberId) {
        return ownerId.equals(memberId);
    }

    public boolean isManager(Long memberId) {
        return members.stream()
                .filter(member -> member.getMemberId().equals(memberId))
                .anyMatch(GroupMember::isManager);
    }

    public boolean hasMember(Long memberId) {
        return members.stream().anyMatch(member -> member.getMemberId().equals(memberId));
    }

    public GroupMember getMember(Long memberId) {
        return findMember(memberId);
    }

    private void addOwner(Long ownerId) {
        members.add(GroupMember.owner(this, ownerId));
        memberCount = 1;
    }

    private void replaceInterests(List<Long> interestIds) {
        interests.clear();
        if (interestIds == null) {
            return;
        }
        interestIds.stream()
                .distinct()
                .forEach(interestId -> interests.add(GroupInterest.of(this, interestId)));
    }

    private void validateRoleTitleUnique(String title, Long roleId) {
        boolean duplicate = roles.stream()
                .filter(role -> roleId == null || !role.getId().equals(roleId))
                .anyMatch(role -> role.getTitle().equalsIgnoreCase(title));
        if (duplicate) {
            throw new BadRequestException(GroupExceptionMessage.GROUP_ROLE_ALREADY_EXISTS.getMessage());
        }
    }

    private void validateMemberAbsent(Long memberId) {
        if (hasMember(memberId)) {
            throw new BadRequestException(GroupExceptionMessage.GROUP_MEMBER_ALREADY_EXISTS.getMessage());
        }
    }

    private void validateMemberCapacity() {
        if (maxMembers != null && memberCount >= maxMembers) {
            throw new BadRequestException(GroupExceptionMessage.GROUP_MEMBER_LIMIT_EXCEEDED.getMessage());
        }
    }

    private void validateMaxMembers(Integer maxMembers) {
        if (maxMembers != null && maxMembers < memberCount) {
            throw new BadRequestException(GroupExceptionMessage.GROUP_MEMBER_LIMIT_EXCEEDED.getMessage());
        }
    }

    private void validateCustomTitle(Long customTitleId) {
        if (customTitleId == null) {
            return;
        }
        findRole(customTitleId);
    }

    private GroupRole findRole(Long roleId) {
        return roles.stream()
                .filter(role -> role.getId().equals(roleId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(GroupExceptionMessage.GROUP_ROLE_NOT_FOUND.getMessage()));
    }

    private GroupMember findMember(Long memberId) {
        return members.stream()
                .filter(member -> member.getMemberId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(GroupExceptionMessage.GROUP_MEMBER_NOT_FOUND.getMessage()));
    }
}
