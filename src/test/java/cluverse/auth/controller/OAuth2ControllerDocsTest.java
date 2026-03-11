package cluverse.auth.controller;

import cluverse.auth.client.GoogleOAuth2Client;
import cluverse.auth.client.KakaoOAuth2Client;
import cluverse.auth.client.OAuthUserInfo;
import cluverse.auth.service.AuthService;
import cluverse.docs.RestDocsSupport;
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

    private final KakaoOAuth2Client kakaoOAuth2Client = mock(KakaoOAuth2Client.class);
    private final GoogleOAuth2Client googleOAuth2Client = mock(GoogleOAuth2Client.class);
    private final AuthService authService = mock(AuthService.class);

    @Override
    protected Object initController() {
        OAuth2Controller controller = new OAuth2Controller(kakaoOAuth2Client, googleOAuth2Client, authService);
        ReflectionTestUtils.setField(controller, "frontendUrl", "http://localhost:3000");
        return controller;
    }

    @Test
    void 지원하지_않는_provider_인증_실패() throws Exception {
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
        when(kakaoOAuth2Client.getUserInfo("auth-code")).thenReturn(userInfo);

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
