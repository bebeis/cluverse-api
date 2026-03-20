package cluverse.member.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    private Integer entranceYear;

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

    public void update(String bio, Integer entranceYear, String profileImageUrl,
                       String linkGithub, String linkNotion, String linkPortfolio,
                       String linkInstagram, String linkEtc,
                       boolean isPublic, List<MemberProfileField> visibleFields) {
        this.bio = bio;
        this.entranceYear = entranceYear;
        this.profileImageUrl = profileImageUrl;
        this.linkGithub = linkGithub;
        this.linkNotion = linkNotion;
        this.linkPortfolio = linkPortfolio;
        this.linkInstagram = linkInstagram;
        this.linkEtc = linkEtc;
        this.isPublic = isPublic;
        this.visibleFields = serializeVisibleFields(visibleFields);
    }

    public boolean canView(MemberProfileField field, boolean sameMember) {
        if (sameMember || isPublic) {
            return true;
        }
        return getVisibleFields().contains(field);
    }

    public Set<MemberProfileField> getVisibleFields() {
        if (visibleFields == null || visibleFields.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(visibleFields.replace("[", "")
                        .replace("]", "")
                        .replace("\"", "")
                        .split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(MemberProfileField::from)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                        java.util.Collections::unmodifiableSet
                ));
    }

    private String serializeVisibleFields(List<MemberProfileField> visibleFields) {
        if (visibleFields == null || visibleFields.isEmpty()) {
            return "[]";
        }
        return visibleFields.stream()
                .map(MemberProfileField::name)
                .map(field -> "\"" + field + "\"")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }
}
