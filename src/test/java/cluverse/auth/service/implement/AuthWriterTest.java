package cluverse.auth.service.implement;

import cluverse.auth.client.OAuthUserInfo;
import cluverse.member.domain.Member;
import cluverse.member.domain.OAuthProvider;
import cluverse.member.repository.MemberQueryRepository;
import cluverse.member.repository.MemberRepository;
import cluverse.member.repository.MemberTermsAgreementRepository;
import cluverse.member.repository.TermsRepository;
import cluverse.university.repository.UniversityRepository;
import cluverse.common.config.PasswordConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthWriterTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberQueryRepository memberQueryRepository;

    @Mock
    private TermsRepository termsRepository;

    @Mock
    private MemberTermsAgreementRepository memberTermsAgreementRepository;

    @Mock
    private UniversityRepository universityRepository;

    @Mock
    private PasswordConfig passwordConfig;

    @InjectMocks
    private AuthWriter authWriter;

    @Test
    void OAuth_로그인_신규_회원_자동_가입() {
        OAuthUserInfo userInfo = new OAuthUserInfo("new-provider-id", "new@example.com", "newuser");
        when(memberRepository.existsByNickname("newuser")).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Member result = authWriter.registerBySocial(userInfo, OAuthProvider.GOOGLE);

        assertThat(result.getNickname()).isEqualTo("newuser");
        assertThat(result.getMemberAuth().getEmail()).isEqualTo("new@example.com");
        assertThat(result.getSocialAccounts()).hasSize(1);
    }
}
