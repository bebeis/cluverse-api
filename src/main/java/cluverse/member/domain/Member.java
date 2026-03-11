package cluverse.member.domain;

import cluverse.common.entity.BaseTimeEntity;
import cluverse.member.exception.MemberExceptionMessage;
import cluverse.common.exception.BadRequestException;
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

    @Column(name = "university_id", nullable = false)
    private Long universityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status = MemberStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.NONE;

    @Column(name = "verification_rejected_reason")
    private String verificationRejectedReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role = MemberRole.MEMBER;

    private LocalDateTime lastLoginAt;
    private String clientIp;

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private MemberAuth memberAuth;

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private MemberProfile profile;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<SocialAccount> socialAccounts = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberMajor> majors = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberInterest> interests = new ArrayList<>();

    private Member(String nickname, Long universityId) {
        this.nickname = nickname;
        this.universityId = universityId;
    }

    public static Member create(String nickname, Long universityId) {
        return new Member(nickname, universityId);
    }

    public void initMemberAuth(String email, String passwordHash) {
        this.memberAuth = MemberAuth.ofEmail(this, email, passwordHash);
    }

    public void initMemberAuthBySocial(String email) {
        this.memberAuth = MemberAuth.ofSocial(this, email);
    }

    public void initProfile(MemberProfile profile) {
        this.profile = profile;
    }

    public void addSocialAccount(OAuthProvider provider, String providerUserId) {
        this.socialAccounts.add(SocialAccount.of(this, provider, providerUserId));
    }

    public void addMajor(Long majorId, MajorType majorType) {
        boolean alreadyExists = majors.stream().anyMatch(m -> m.getMajorId().equals(majorId));
        if (alreadyExists) {
            throw new BadRequestException(MemberExceptionMessage.MAJOR_ALREADY_REGISTERED.getMessage());
        }
        majors.add(MemberMajor.of(this, majorId, majorType));
    }

    public void removeMajor(Long majorId) {
        MemberMajor target = majors.stream()
                .filter(m -> m.getMajorId().equals(majorId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(MemberExceptionMessage.MAJOR_NOT_FOUND.getMessage()));

        if (target.getMajorType() == MajorType.PRIMARY) {
            long primaryCount = majors.stream().filter(m -> m.getMajorType() == MajorType.PRIMARY).count();
            if (primaryCount <= 1) {
                throw new BadRequestException(MemberExceptionMessage.PRIMARY_MAJOR_REQUIRED.getMessage());
            }
        }
        majors.remove(target);
    }

    public void addInterest(Long interestId) {
        boolean alreadyExists = interests.stream().anyMatch(i -> i.getInterestId().equals(interestId));
        if (alreadyExists) {
            throw new BadRequestException(MemberExceptionMessage.INTEREST_ALREADY_REGISTERED.getMessage());
        }
        interests.add(MemberInterest.of(this, interestId));
    }

    public void removeInterest(Long interestId) {
        MemberInterest target = interests.stream()
                .filter(i -> i.getInterestId().equals(interestId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(MemberExceptionMessage.INTEREST_NOT_FOUND.getMessage()));
        interests.remove(target);
    }

    public void updateLastLogin(String clientIp) {
        this.lastLoginAt = LocalDateTime.now();
        this.clientIp = clientIp;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void requestVerification() {
        this.verificationStatus = VerificationStatus.PENDING;
    }

    public void approveVerification() {
        this.verificationStatus = VerificationStatus.APPROVED;
        this.verificationRejectedReason = null;
    }

    public void rejectVerification(String reason) {
        this.verificationStatus = VerificationStatus.REJECTED;
        this.verificationRejectedReason = reason;
    }

    public boolean isVerified() {
        return this.verificationStatus == VerificationStatus.APPROVED;
    }

    public boolean isActive() {
        return this.status == MemberStatus.ACTIVE;
    }
}
