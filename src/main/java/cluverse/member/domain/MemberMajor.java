package cluverse.member.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberMajor extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_major_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "major_id", nullable = false)
    private Long majorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MajorType majorType;

    private MemberMajor(Member member, Long majorId, MajorType majorType) {
        this.member = member;
        this.majorId = majorId;
        this.majorType = majorType;
    }

    public static MemberMajor of(Member member, Long majorId, MajorType majorType) {
        return new MemberMajor(member, majorId, majorType);
    }
}
