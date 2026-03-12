package cluverse.auth.service.implement;

import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.common.config.PasswordConfig;
import cluverse.common.exception.UnauthorizedException;
import cluverse.member.domain.Member;
import cluverse.member.domain.OAuthProvider;
import cluverse.member.repository.MemberQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthReader {

    private final MemberQueryRepository memberQueryRepository;
    private final PasswordConfig passwordConfig;

    public Member readByEmailAndPassword(String email, String rawPassword) {
        Member member = memberQueryRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException(AuthExceptionMessage.INVALID_CREDENTIALS.getMessage()));
        if (!passwordConfig.matches(rawPassword, member.getMemberAuth().getPasswordHash())) {
            throw new UnauthorizedException(AuthExceptionMessage.INVALID_CREDENTIALS.getMessage());
        }
        return member;
    }

    public Optional<Member> findBySocialAccount(OAuthProvider provider, String providerUserId) {
        return memberQueryRepository.findBySocialAccount(provider, providerUserId);
    }
}
