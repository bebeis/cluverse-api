package cluverse.member.controller;

import cluverse.common.auth.LoginMember;
import cluverse.docs.RestDocsSupport;
import cluverse.member.domain.MemberRole;
import cluverse.member.domain.StudentVerificationMethod;
import cluverse.member.domain.VerificationStatus;
import cluverse.member.service.StudentVerificationQueryService;
import cluverse.member.service.StudentVerificationService;
import cluverse.member.service.request.StudentVerificationEmailChallengeCreateRequest;
import cluverse.member.service.request.StudentVerificationEmailConfirmationCreateRequest;
import cluverse.member.service.response.StudentVerificationEmailChallengeResponse;
import cluverse.member.service.response.StudentVerificationStatusResponse;
import cluverse.member.service.response.StudentVerificationUniversityResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;

import static cluverse.common.auth.LoginMemberArgumentResolver.SESSION_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentVerificationControllerDocsTest extends RestDocsSupport {

    private final StudentVerificationQueryService studentVerificationQueryService = mock(StudentVerificationQueryService.class);
    private final StudentVerificationService studentVerificationService = mock(StudentVerificationService.class);

    @Override
    protected Object initController() {
        return new StudentVerificationController(studentVerificationQueryService, studentVerificationService);
    }

    @Test
    void 내_학생_인증_상태_조회() throws Exception {
        when(studentVerificationQueryService.getVerificationStatus(1L))
                .thenReturn(createPendingVerificationStatusResponse());

        mockMvc.perform(get("/api/v1/members/me/student-verification")
                        .session(createSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verificationStatus").value("PENDING"))
                .andDo(document("members/get-student-verification-status",
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.verificationStatus").type(JsonFieldType.STRING)
                                        .description("학생 인증 상태 (NONE, PENDING, APPROVED, REJECTED)"),
                                fieldWithPath("data.verified").type(JsonFieldType.BOOLEAN).description("학생 인증 완료 여부"),
                                fieldWithPath("data.verificationMethod").type(JsonFieldType.STRING)
                                        .description("학생 인증 방식 (예: SCHOOL_EMAIL)").optional(),
                                fieldWithPath("data.university.universityId").type(JsonFieldType.NUMBER)
                                        .description("인증 기준 학교 ID").optional(),
                                fieldWithPath("data.university.universityName").type(JsonFieldType.STRING)
                                        .description("인증 기준 학교명").optional(),
                                fieldWithPath("data.university.emailDomain").type(JsonFieldType.STRING)
                                        .description("허용된 학교 이메일 도메인").optional(),
                                fieldWithPath("data.schoolEmail").type(JsonFieldType.STRING)
                                        .description("인증 진행 또는 완료에 사용한 학교 이메일").optional(),
                                fieldWithPath("data.rejectedReason").type(JsonFieldType.STRING)
                                        .description("인증 거절 사유 코드").optional(),
                                fieldWithPath("data.requestedAt").type(JsonFieldType.STRING)
                                        .description("최근 인증 요청 시각").optional(),
                                fieldWithPath("data.verifiedAt").type(JsonFieldType.STRING)
                                        .description("학생 인증 완료 시각").optional()
                        )
                ));
    }

    @Test
    void 학교_이메일_인증_challenge_생성() throws Exception {
        when(studentVerificationService.createEmailChallenge(
                eq(1L),
                any(StudentVerificationEmailChallengeCreateRequest.class)
        )).thenReturn(new StudentVerificationEmailChallengeResponse(
                "evc_01HX8Q9K7Z2M4N6P8R0STUVWX1",
                VerificationStatus.PENDING,
                "luna@snu.ac.kr",
                LocalDateTime.of(2026, 4, 16, 15, 5),
                60L
        ));

        mockMvc.perform(post("/api/v1/members/me/student-verification/email-challenges")
                        .session(createSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "luna@snu.ac.kr"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.challengeId").value("evc_01HX8Q9K7Z2M4N6P8R0STUVWX1"))
                .andDo(document("members/create-student-verification-email-challenge",
                        requestFields(
                                fieldWithPath("email").type(JsonFieldType.STRING)
                                        .description("인증 코드를 받을 학교 이메일")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.challengeId").type(JsonFieldType.STRING)
                                        .description("학교 이메일 인증 시도 식별자"),
                                fieldWithPath("data.verificationStatus").type(JsonFieldType.STRING)
                                        .description("학생 인증 상태"),
                                fieldWithPath("data.email").type(JsonFieldType.STRING)
                                        .description("인증 코드가 발송된 학교 이메일"),
                                fieldWithPath("data.expiresAt").type(JsonFieldType.STRING)
                                        .description("인증 코드 만료 시각"),
                                fieldWithPath("data.retryAfterSeconds").type(JsonFieldType.NUMBER)
                                        .description("재발송 가능까지 남은 초")
                        )
                ));
    }

    @Test
    void 학교_이메일_인증_challenge_생성시_이메일_형식이_아니면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/members/me/student-verification/email-challenges")
                        .session(createSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andDo(document("members/create-student-verification-email-challenge-validation-fail",
                        requestFields(
                                fieldWithPath("email").type(JsonFieldType.STRING)
                                        .description("학교 이메일 (이메일 형식 필수)")
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
    void 학교_이메일_인증_confirmation_생성() throws Exception {
        when(studentVerificationService.createEmailConfirmation(
                eq(1L),
                eq("evc_01HX8Q9K7Z2M4N6P8R0STUVWX1"),
                any(StudentVerificationEmailConfirmationCreateRequest.class)
        )).thenReturn(createApprovedVerificationStatusResponse());

        mockMvc.perform(post(
                        "/api/v1/members/me/student-verification/email-challenges/{challengeId}/confirmations",
                        "evc_01HX8Q9K7Z2M4N6P8R0STUVWX1"
                )
                        .session(createSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "code": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verificationStatus").value("APPROVED"))
                .andDo(document("members/create-student-verification-email-confirmation",
                        pathParameters(
                                parameterWithName("challengeId").description("학교 이메일 인증 시도 식별자")
                        ),
                        requestFields(
                                fieldWithPath("code").type(JsonFieldType.STRING)
                                        .description("메일로 전달된 6자리 인증 코드")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.verificationStatus").type(JsonFieldType.STRING)
                                        .description("학생 인증 상태"),
                                fieldWithPath("data.verified").type(JsonFieldType.BOOLEAN).description("학생 인증 완료 여부"),
                                fieldWithPath("data.verificationMethod").type(JsonFieldType.STRING)
                                        .description("학생 인증 방식"),
                                fieldWithPath("data.university.universityId").type(JsonFieldType.NUMBER)
                                        .description("인증 완료된 학교 ID"),
                                fieldWithPath("data.university.universityName").type(JsonFieldType.STRING)
                                        .description("인증 완료된 학교명"),
                                fieldWithPath("data.university.emailDomain").type(JsonFieldType.STRING)
                                        .description("학교 이메일 도메인"),
                                fieldWithPath("data.schoolEmail").type(JsonFieldType.STRING)
                                        .description("인증 완료된 학교 이메일"),
                                fieldWithPath("data.rejectedReason").type(JsonFieldType.NULL)
                                        .description("인증 거절 사유 없음"),
                                fieldWithPath("data.requestedAt").type(JsonFieldType.STRING)
                                        .description("인증 코드 요청 시각"),
                                fieldWithPath("data.verifiedAt").type(JsonFieldType.STRING)
                                        .description("학생 인증 완료 시각")
                        )
                ));
    }

    private MockHttpSession createSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY, new LoginMember(1L, "luna", MemberRole.MEMBER));
        return session;
    }

    private StudentVerificationStatusResponse createPendingVerificationStatusResponse() {
        return new StudentVerificationStatusResponse(
                VerificationStatus.PENDING,
                false,
                StudentVerificationMethod.SCHOOL_EMAIL,
                new StudentVerificationUniversityResponse(10L, "서울대학교", "snu.ac.kr"),
                "luna@snu.ac.kr",
                null,
                LocalDateTime.of(2026, 4, 16, 14, 55),
                null
        );
    }

    private StudentVerificationStatusResponse createApprovedVerificationStatusResponse() {
        return new StudentVerificationStatusResponse(
                VerificationStatus.APPROVED,
                true,
                StudentVerificationMethod.SCHOOL_EMAIL,
                new StudentVerificationUniversityResponse(10L, "서울대학교", "snu.ac.kr"),
                "luna@snu.ac.kr",
                null,
                LocalDateTime.of(2026, 4, 16, 14, 55),
                LocalDateTime.of(2026, 4, 16, 15, 0)
        );
    }
}
