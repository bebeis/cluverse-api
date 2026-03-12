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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Member result = authWriter.registerBySocial(userInfo, OAuthProvider.GOOGLE);

        assertThat(result.getNickname()).startsWith("newuser_");
        assertThat(result.getNickname()).hasSizeLessThanOrEqualTo(50);
        assertThat(result.getUniversityId()).isNull();
        assertThat(result.getMemberAuth().getEmail()).isEqualTo("new@example.com");
        assertThat(result.getSocialAccounts()).hasSize(1);
        verify(memberRepository, never()).existsByNickname(any());
    }

    @Test
    void OAuth_로그인_닉네임은_최대_길이를_초과하지_않는다() {
        OAuthUserInfo userInfo = new OAuthUserInfo("provider-id", "new@example.com", "a".repeat(100));
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Member result = authWriter.registerBySocial(userInfo, OAuthProvider.GOOGLE);

        assertThat(result.getNickname()).hasSizeLessThanOrEqualTo(50);
        assertThat(result.getNickname()).matches("a{41}_[0-9a-f]{8}");
    }
}
