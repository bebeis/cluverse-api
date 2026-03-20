package cluverse.group.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_member_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupMemberRole role;

    @Column(name = "custom_title_id")
    private Long customTitleId;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    private GroupMember(Group group, Long memberId, GroupMemberRole role) {
        this.group = group;
        this.memberId = memberId;
        this.role = role;
        this.joinedAt = LocalDateTime.now();
    }

    public static GroupMember owner(Group group, Long memberId) {
        return new GroupMember(group, memberId, GroupMemberRole.OWNER);
    }

    public static GroupMember member(Group group, Long memberId) {
        return new GroupMember(group, memberId, GroupMemberRole.MEMBER);
    }

    public void update(GroupMemberRole role, Long customTitleId) {
        this.role = role;
        this.customTitleId = customTitleId;
    }

    public void assignOwner() {
        this.role = GroupMemberRole.OWNER;
    }

    public void changeRole(GroupMemberRole role) {
        this.role = role;
    }

    public void clearCustomTitle() {
        this.customTitleId = null;
    }

    public void assignCustomTitle(Long customTitleId) {
        this.customTitleId = customTitleId;
    }

    public boolean isOwner() {
        return role == GroupMemberRole.OWNER;
    }

    public boolean isManager() {
        return role == GroupMemberRole.OWNER || role == GroupMemberRole.ADMIN;
    }
}
