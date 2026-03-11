package cluverse.member.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status = MemberStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role = MemberRole.MEMBER;

    private LocalDateTime lastLoginAt;
    private String clientIp;

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private MemberAuth memberAuth;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<SocialAccount> socialAccounts = new ArrayList<>();

    private Member(String nickname) {
        this.nickname = nickname;
    }

    public static Member create(String nickname) {
        return new Member(nickname);
    }

    public void initMemberAuthBySocial(String email) {
        this.memberAuth = MemberAuth.ofSocial(this, email);
    }

    public void addSocialAccount(OAuthProvider provider, String providerUserId) {
        this.socialAccounts.add(SocialAccount.of(this, provider, providerUserId));
    }

    public void updateLastLogin(String clientIp) {
        this.lastLoginAt = LocalDateTime.now();
        this.clientIp = clientIp;
    }
}
