package cluverse.auth.service.implement;

import cluverse.auth.client.OAuthUserInfo;
import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.member.domain.Member;
import cluverse.member.domain.MemberProfile;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
        when(memberQueryRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(memberRepository.existsByNickname(anyString())).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Member result = authWriter.registerBySocial(userInfo, OAuthProvider.GOOGLE);

        assertThat(result.getNickname()).startsWith("newuser_");
        assertThat(result.getNickname()).hasSizeLessThanOrEqualTo(50);
        assertThat(result.getUniversityId()).isNull();
        assertThat(result.isVerified()).isTrue();
        assertThat(result.getMemberAuth().getEmail()).isEqualTo("new@example.com");
        assertThat(result.getSocialAccounts()).hasSize(1);
        verify(memberRepository).existsByNickname(anyString());
    }

    @Test
    void OAuth_로그인_닉네임은_최대_길이를_초과하지_않는다() {
        OAuthUserInfo userInfo = new OAuthUserInfo("provider-id", "new@example.com", "a".repeat(100));
        when(memberQueryRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(memberRepository.existsByNickname(anyString())).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Member result = authWriter.registerBySocial(userInfo, OAuthProvider.GOOGLE);

        assertThat(result.getNickname()).hasSizeLessThanOrEqualTo(50);
        assertThat(result.getNickname()).matches("a{41}_[0-9a-f]{8}");
    }

    @Test
    void OAuth_로그인시_같은_이메일의_기존_회원에_소셜_계정을_연결한다() {
        OAuthUserInfo userInfo = new OAuthUserInfo("google-user-1", "linked@example.com", "social-user");
        Member member = Member.create("luna", 10L);
        ReflectionTestUtils.setField(member, "id", 1L);
        member.initMemberAuth("linked@example.com", "encoded-password");
        member.initProfile(MemberProfile.create(member));

        when(memberQueryRepository.findByEmail("linked@example.com")).thenReturn(Optional.of(member));
        when(memberRepository.save(member)).thenReturn(member);

        Member result = authWriter.registerBySocial(userInfo, OAuthProvider.GOOGLE);

        assertThat(result).isSameAs(member);
        assertThat(result.getSocialAccounts()).hasSize(1);
        assertThat(result.getSocialAccounts().getFirst().getProvider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(result.getSocialAccounts().getFirst().getProviderUserId()).isEqualTo("google-user-1");
        assertThat(result.getMemberAuth().getEmail()).isEqualTo("linked@example.com");
        verify(memberRepository, never()).existsByNickname(anyString());
    }

    @Test
    void OAuth_로그인_닉네임이_충돌하면_다른_후보로_재시도한다() {
        OAuthUserInfo userInfo = new OAuthUserInfo("retry-provider-id", "retry@example.com", "retry-user");
        when(memberQueryRepository.findByEmail("retry@example.com")).thenReturn(Optional.empty());
        when(memberRepository.existsByNickname(anyString())).thenReturn(true, false);
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Member result = authWriter.registerBySocial(userInfo, OAuthProvider.KAKAO);

        assertThat(result.getNickname()).startsWith("retry-user_");
        verify(memberRepository, times(2)).existsByNickname(anyString());
    }

    @Test
    void OAuth_로그인_닉네임을_끝내_생성하지_못하면_예외가_발생한다() {
        OAuthUserInfo userInfo = new OAuthUserInfo("retry-provider-id", "retry@example.com", "retry-user");
        when(memberQueryRepository.findByEmail("retry@example.com")).thenReturn(Optional.empty());
        when(memberRepository.existsByNickname(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authWriter.registerBySocial(userInfo, OAuthProvider.KAKAO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(AuthExceptionMessage.SOCIAL_NICKNAME_GENERATION_FAILED.getMessage());
    }
}
