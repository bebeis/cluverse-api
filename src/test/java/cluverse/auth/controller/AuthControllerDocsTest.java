package cluverse.auth.controller;

import cluverse.auth.service.AuthService;
import cluverse.common.auth.LoginMember;
import cluverse.common.auth.LoginSessionManager;
import cluverse.common.exception.UnauthorizedException;
import cluverse.docs.RestDocsSupport;
import cluverse.member.domain.MemberRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerDocsTest extends RestDocsSupport {

    private final AuthService authService = mock(AuthService.class);
    private final LoginSessionManager loginSessionManager = mock(LoginSessionManager.class);

    @Override
    protected Object initController() {
        return new AuthController(authService, loginSessionManager);
    }

    @Test
    void 회원가입_성공() throws Exception {
        LoginMember loginMember = new LoginMember(1L, "testuser", MemberRole.MEMBER);
        when(authService.register(any(), anyString())).thenReturn(loginMember);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@example.com",
                                    "password": "password123",
                                    "nickname": "testuser",
                                    "universityId": 10,
                                    "agreedTermsIds": [1, 2, 3]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.memberId").value(1))
                .andDo(document("auth/register",
                        requestFields(
                                fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                                fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호"),
                                fieldWithPath("nickname").type(JsonFieldType.STRING).description("닉네임"),
                                fieldWithPath("universityId").type(JsonFieldType.NUMBER).description("학교 ID"),
                                fieldWithPath("agreedTermsIds").type(JsonFieldType.ARRAY).description("동의한 약관 ID 목록")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.memberId").type(JsonFieldType.NUMBER).description("회원 ID"),
                                fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("닉네임"),
                                fieldWithPath("data.role").type(JsonFieldType.STRING).description("역할")
                        )
                ));
    }

    @Test
    void 이메일_로그인_성공() throws Exception {
        LoginMember loginMember = new LoginMember(1L, "testuser", MemberRole.MEMBER);
        when(authService.loginWithEmail(anyString(), anyString(), anyString()))
                .thenReturn(loginMember);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@example.com",
                                    "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.memberId").value(1))
                .andExpect(jsonPath("$.data.nickname").value("testuser"))
                .andDo(document("auth/login",
                        requestFields(
                                fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                                fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.memberId").type(JsonFieldType.NUMBER).description("회원 ID"),
                                fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("닉네임"),
                                fieldWithPath("data.role").type(JsonFieldType.STRING).description("역할")
                        )
                ));
    }

    @Test
    void 이메일_로그인_실패_잘못된_자격증명() throws Exception {
        when(authService.loginWithEmail(anyString(), anyString(), anyString()))
                .thenThrow(new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다."));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "wrong@example.com",
                                    "password": "wrongpass"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andDo(document("auth/login-fail",
                        requestFields(
                                fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                                fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호")
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
    void 이메일_로그인_실패_유효성_검사() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "not-an-email",
                                    "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andDo(document("auth/login-validation-fail",
                        requestFields(
                                fieldWithPath("email").type(JsonFieldType.STRING).description("이메일 (이메일 형식 필수)"),
                                fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호 (공백 불가)")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("유효성 검사 에러 메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("데이터 없음")
                        )
                ));
    }

    @Test
    void 로그아웃_성공() throws Exception {
        doNothing().when(loginSessionManager).invalidateSession(any());

        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andDo(document("auth/logout",
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("데이터 없음")
                        )
                ));
    }

    @Test
    void OAuth_토큰_교환_성공() throws Exception {
        LoginMember loginMember = new LoginMember(1L, "kakaouser", MemberRole.MEMBER);
        when(authService.exchangeOAuthToken("valid-token")).thenReturn(loginMember);

        mockMvc.perform(post("/api/v1/auth/oauth/token")
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(1))
                .andDo(document("auth/oauth-token",
                        queryParameters(
                                parameterWithName("token").description("소셜 로그인 콜백에서 전달받은 임시 OAuth 토큰")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.memberId").type(JsonFieldType.NUMBER).description("회원 ID"),
                                fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("닉네임"),
                                fieldWithPath("data.role").type(JsonFieldType.STRING).description("역할")
                        )
                ));
    }

    @Test
    void OAuth_토큰_교환_실패_유효하지_않은_토큰() throws Exception {
        when(authService.exchangeOAuthToken("invalid-token"))
                .thenThrow(new UnauthorizedException("유효하지 않거나 만료된 OAuth 인증 토큰입니다."));

        mockMvc.perform(post("/api/v1/auth/oauth/token")
                        .param("token", "invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andDo(document("auth/oauth-token-fail",
                        queryParameters(
                                parameterWithName("token").description("소셜 로그인 콜백에서 전달받은 임시 OAuth 토큰")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("데이터 없음")
                        )
                ));
    }
}
