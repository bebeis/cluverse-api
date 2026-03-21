package cluverse.member.service.implement;

import cluverse.member.domain.Member;
import cluverse.member.domain.MemberTermsAgreement;
import cluverse.member.repository.MemberTermsAgreementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class MemberTermsAgreementWriter {

    private final MemberTermsAgreementRepository memberTermsAgreementRepository;

    public void save(Member member, Long termsId) {
        memberTermsAgreementRepository.save(MemberTermsAgreement.of(member, termsId));
    }
}
