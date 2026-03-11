package cluverse.member.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberProfile extends BaseTimeEntity {

    @Id
    private Long memberId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String profileImageUrl;
    private String linkGithub;
    private String linkNotion;
    private String linkPortfolio;
    private String linkInstagram;
    private String linkEtc;

    @Column(nullable = false)
    private boolean isPublic = true;

    @Column(columnDefinition = "JSON")
    private String visibleFields;

    private MemberProfile(Member member) {
        this.member = member;
    }

    public static MemberProfile create(Member member) {
        return new MemberProfile(member);
    }

    public void update(String bio, String profileImageUrl,
                       String linkGithub, String linkNotion, String linkPortfolio,
                       String linkInstagram, String linkEtc,
                       boolean isPublic, String visibleFields) {
        this.bio = bio;
        this.profileImageUrl = profileImageUrl;
        this.linkGithub = linkGithub;
        this.linkNotion = linkNotion;
        this.linkPortfolio = linkPortfolio;
        this.linkInstagram = linkInstagram;
        this.linkEtc = linkEtc;
        this.isPublic = isPublic;
        this.visibleFields = visibleFields;
    }
}
