package cluverse.auth.controller;

import cluverse.auth.client.OAuth2Client;
import cluverse.auth.client.OAuth2ClientManager;
import cluverse.auth.client.OAuthUserInfo;
import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.auth.service.AuthService;
import cluverse.common.auth.LoginMember;
import cluverse.common.auth.LoginSessionManager;
import cluverse.common.exception.BadRequestException;
import cluverse.docs.RestDocsSupport;
import cluverse.member.domain.MemberRole;
import cluverse.member.domain.OAuthProvider;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OAuth2ControllerDocsTest extends RestDocsSupport {

    private final OAuth2ClientManager oAuth2ClientManager = mock(OAuth2ClientManager.class);
    private final OAuth2Client oAuth2Client = mock(OAuth2Client.class);
    private final AuthService authService = mock(AuthService.class);
    private final LoginSessionManager loginSessionManager = mock(LoginSessionManager.class);

    @Override
    protected Object initController() {
        OAuth2Controller controller = new OAuth2Controller(oAuth2ClientManager, authService, loginSessionManager);
        ReflectionTestUtils.setField(controller, "frontendUrl", "http://localhost:3000");
        return controller;
    }

    @Test
    void 지원하지_않는_provider_인증_실패() throws Exception {
        when(oAuth2ClientManager.getClient("unknown"))
                .thenThrow(new BadRequestException(AuthExceptionMessage.UNSUPPORTED_OAUTH_PROVIDER.getMessage()));

        mockMvc.perform(get("/oauth2/{provider}", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andDo(document("oauth2/authorize-fail",
                        pathParameters(
                                parameterWithName("provider").description("OAuth2 provider (kakao, google)")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("데이터 없음")
                        )
                ));
    }

    @Test
    void 카카오_콜백_성공() throws Exception {
        OAuthUserInfo userInfo = new OAuthUserInfo("kakao-id", "kakao@example.com", "kakaouser");
        when(oAuth2ClientManager.getClient("kakao")).thenReturn(oAuth2Client);
        when(oAuth2Client.getUserInfo("auth-code")).thenReturn(userInfo);
        when(oAuth2Client.provider()).thenReturn(OAuthProvider.KAKAO);
        when(authService.loginWithOAuth(userInfo, OAuthProvider.KAKAO, "127.0.0.1"))
                .thenReturn(new LoginMember(1L, "kakaouser", MemberRole.MEMBER));

        mockMvc.perform(get("/oauth2/{provider}/callback", "kakao")
                        .param("code", "auth-code"))
                .andExpect(status().is3xxRedirection())
                .andDo(document("oauth2/callback",
                        pathParameters(
                                parameterWithName("provider").description("OAuth2 provider (kakao, google)")
                        )
                ));
    }
}
