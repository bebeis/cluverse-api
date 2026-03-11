package cluverse.member.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberStatusHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_status_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // SCD Type 3으로 저장
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus newStatus;

    @Column(nullable = false)
    private String changeType;

    @Column(columnDefinition = "TEXT")
    private String changeReason;

    @Column(name = "changed_by")
    private Long changedBy;

    private MemberStatusHistory(Member member, MemberStatus previousStatus, MemberStatus newStatus,
                                String changeType, String changeReason, Long changedBy) {
        this.member = member;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changeType = changeType;
        this.changeReason = changeReason;
        this.changedBy = changedBy;
    }

    public static MemberStatusHistory of(Member member, MemberStatus previousStatus, MemberStatus newStatus,
                                         String changeType, String changeReason, Long changedBy) {
        return new MemberStatusHistory(member, previousStatus, newStatus, changeType, changeReason, changedBy);
    }
}
