package cluverse.member.repository;

import cluverse.member.domain.MemberTermsAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberTermsAgreementRepository extends JpaRepository<MemberTermsAgreement, Long> {

    List<MemberTermsAgreement> findAllByMemberId(Long memberId);
}
