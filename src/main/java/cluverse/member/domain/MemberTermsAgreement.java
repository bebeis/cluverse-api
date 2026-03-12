package cluverse.member.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberTermsAgreement extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_terms_agreement_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "terms_id", nullable = false)
    private Long termsId;

    @Column(nullable = false)
    private LocalDateTime agreedAt;

    private MemberTermsAgreement(Member member, Long termsId, LocalDateTime agreedAt) {
        this.member = member;
        this.termsId = termsId;
        this.agreedAt = agreedAt;
    }

    public static MemberTermsAgreement of(Member member, Long termsId) {
        return new MemberTermsAgreement(member, termsId, LocalDateTime.now());
    }
}
