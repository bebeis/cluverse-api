package cluverse.member.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberAuth extends BaseTimeEntity {

    @Id
    private Long memberId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false, unique = true)
    private String email;

    private String passwordHash;

    private MemberAuth(Member member, String email, String passwordHash) {
        this.member = member;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public static MemberAuth ofEmail(Member member, String email, String passwordHash) {
        return new MemberAuth(member, email, passwordHash);
    }

    public static MemberAuth ofSocial(Member member, String email) {
        return new MemberAuth(member, email, null);
    }
}
