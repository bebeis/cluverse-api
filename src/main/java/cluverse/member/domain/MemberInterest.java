package cluverse.member.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberInterest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_interest_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "interest_id", nullable = false)
    private Long interestId;

    private MemberInterest(Member member, Long interestId) {
        this.member = member;
        this.interestId = interestId;
    }

    public static MemberInterest of(Member member, Long interestId) {
        return new MemberInterest(member, interestId);
    }
}
