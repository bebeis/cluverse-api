package cluverse.common.auth;

import cluverse.member.domain.MemberRole;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class LoginMemberExtractorTest {

    private final LoginMemberExtractor loginMemberExtractor = new LoginMemberExtractor();

    @Test
    void prefersSessionOverAuthorizationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        LoginMember sessionMember = new LoginMember(1L, "luna", MemberRole.ADMIN);
        request.getSession().setAttribute(LoginMemberArgumentResolver.SESSION_KEY, sessionMember);
        request.addHeader("Authorization", "Bearer 42");

        LoginMember result = loginMemberExtractor.extract(request);

        assertThat(result).isEqualTo(sessionMember);
    }

    @Test
    void createsLoginMemberFromAuthorizationBearer() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer 42");

        LoginMember result = loginMemberExtractor.extract(request);

        assertThat(result).isEqualTo(new LoginMember(42L, null, MemberRole.MEMBER));
    }

    @Test
    void acceptsCaseInsensitiveBearerScheme() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "bearer 42");

        LoginMember result = loginMemberExtractor.extract(request);

        assertThat(result.memberId()).isEqualTo(42L);
    }

    @Test
    void rejectsNonPositiveMemberId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer 0");

        LoginMember result = loginMemberExtractor.extract(request);

        assertThat(result).isNull();
    }

    @Test
    void rejectsNonNumericBearerValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer not-a-member-id");

        LoginMember result = loginMemberExtractor.extract(request);

        assertThat(result).isNull();
    }
}
