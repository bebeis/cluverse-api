package cluverse.university.controller;

import cluverse.common.auth.LoginMember;
import cluverse.docs.RestDocsSupport;
import cluverse.member.domain.MemberRole;
import cluverse.university.service.UniversityService;
import cluverse.university.service.response.UniversityDetailResponse;
import cluverse.university.service.response.UniversitySummaryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;

import static cluverse.common.auth.LoginMemberArgumentResolver.SESSION_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UniversityControllerDocsTest extends RestDocsSupport {

    private final UniversityService universityService = mock(UniversityService.class);

    @Override
    protected Object initController() {
        return new UniversityController(universityService);
    }

    @Test
    void 학교_목록_검색() throws Exception {
        when(universityService.searchUniversities(any())).thenReturn(List.of(
                new UniversitySummaryResponse(1L, "클루대학교", "https://cdn.example.com/universities/clu-badge.png"),
                new UniversitySummaryResponse(2L, "클루공과대학교", "https://cdn.example.com/universities/clu-tech-badge.png")
        ));

        mockMvc.perform(get("/api/v1/universities")
                        .queryParam("keyword", "클루"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].universityId").value(1))
                .andDo(document("universities/search",
                        queryParameters(
                                parameterWithName("keyword").description("학교명 검색어").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data").type(JsonFieldType.ARRAY).description("학교 검색 결과"),
                                fieldWithPath("data[].universityId").type(JsonFieldType.NUMBER).description("학교 ID"),
                                fieldWithPath("data[].universityName").type(JsonFieldType.STRING).description("학교명"),
                                fieldWithPath("data[].universityBadgeImageUrl").type(JsonFieldType.STRING).description("학교 배지 이미지 URL").optional()
                        )
                ));
    }

    @Test
    void 학교_상세_조회() throws Exception {
        when(universityService.getUniversity(1L)).thenReturn(createUniversityDetailResponse());

        mockMvc.perform(get("/api/v1/universities/{universityId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.universityId").value(1))
                .andDo(document("universities/get-detail",
                        pathParameters(
                                parameterWithName("universityId").description("조회할 학교 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.universityId").type(JsonFieldType.NUMBER).description("학교 ID"),
                                fieldWithPath("data.universityName").type(JsonFieldType.STRING).description("학교명"),
                                fieldWithPath("data.emailDomain").type(JsonFieldType.STRING).description("학교 이메일 도메인").optional(),
                                fieldWithPath("data.universityBadgeImageUrl").type(JsonFieldType.STRING).description("학교 배지 이미지 URL").optional(),
                                fieldWithPath("data.address").type(JsonFieldType.STRING).description("학교 주소").optional(),
                                fieldWithPath("data.isActive").type(JsonFieldType.BOOLEAN).description("학교 활성 여부")
                        )
                ));
    }

    @Test
    void 학교_등록() throws Exception {
        when(universityService.createUniversity(eq(1L), any())).thenReturn(createUniversityDetailResponse());

        mockMvc.perform(post("/api/v1/universities")
                        .session(createAdminSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "클루대학교",
                                    "emailDomain": "cluverse.ac.kr",
                                    "badgeImageUrl": "https://cdn.example.com/universities/clu-badge.png",
                                    "address": "서울시 관악구 클루대로 1",
                                    "isActive": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.universityName").value("클루대학교"))
                .andDo(document("universities/create",
                        requestFields(
                                fieldWithPath("name").type(JsonFieldType.STRING).description("학교명"),
                                fieldWithPath("emailDomain").type(JsonFieldType.STRING).description("학교 이메일 도메인").optional(),
                                fieldWithPath("badgeImageUrl").type(JsonFieldType.STRING).description("학교 배지 이미지 URL").optional(),
                                fieldWithPath("address").type(JsonFieldType.STRING).description("학교 주소").optional(),
                                fieldWithPath("isActive").type(JsonFieldType.BOOLEAN).description("학교 활성 여부")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.universityId").type(JsonFieldType.NUMBER).description("학교 ID"),
                                fieldWithPath("data.universityName").type(JsonFieldType.STRING).description("학교명"),
                                fieldWithPath("data.emailDomain").type(JsonFieldType.STRING).description("학교 이메일 도메인").optional(),
                                fieldWithPath("data.universityBadgeImageUrl").type(JsonFieldType.STRING).description("학교 배지 이미지 URL").optional(),
                                fieldWithPath("data.address").type(JsonFieldType.STRING).description("학교 주소").optional(),
                                fieldWithPath("data.isActive").type(JsonFieldType.BOOLEAN).description("학교 활성 여부")
                        )
                ));
    }

    @Test
    void 학교_수정() throws Exception {
        when(universityService.updateUniversity(eq(1L), eq(1L), any())).thenReturn(new UniversityDetailResponse(
                1L,
                "클루대학교",
                "cluverse.ac.kr",
                "https://cdn.example.com/universities/clu-badge-v2.png",
                "서울시 관악구 클루대로 2",
                true
        ));

        mockMvc.perform(put("/api/v1/universities/{universityId}", 1L)
                        .session(createAdminSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "클루대학교",
                                    "emailDomain": "cluverse.ac.kr",
                                    "badgeImageUrl": "https://cdn.example.com/universities/clu-badge-v2.png",
                                    "address": "서울시 관악구 클루대로 2",
                                    "isActive": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.address").value("서울시 관악구 클루대로 2"))
                .andDo(document("universities/update",
                        pathParameters(
                                parameterWithName("universityId").description("수정할 학교 ID")
                        ),
                        requestFields(
                                fieldWithPath("name").type(JsonFieldType.STRING).description("학교명"),
                                fieldWithPath("emailDomain").type(JsonFieldType.STRING).description("학교 이메일 도메인").optional(),
                                fieldWithPath("badgeImageUrl").type(JsonFieldType.STRING).description("학교 배지 이미지 URL").optional(),
                                fieldWithPath("address").type(JsonFieldType.STRING).description("학교 주소").optional(),
                                fieldWithPath("isActive").type(JsonFieldType.BOOLEAN).description("학교 활성 여부")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.universityId").type(JsonFieldType.NUMBER).description("학교 ID"),
                                fieldWithPath("data.universityName").type(JsonFieldType.STRING).description("학교명"),
                                fieldWithPath("data.emailDomain").type(JsonFieldType.STRING).description("학교 이메일 도메인").optional(),
                                fieldWithPath("data.universityBadgeImageUrl").type(JsonFieldType.STRING).description("학교 배지 이미지 URL").optional(),
                                fieldWithPath("data.address").type(JsonFieldType.STRING).description("학교 주소").optional(),
                                fieldWithPath("data.isActive").type(JsonFieldType.BOOLEAN).description("학교 활성 여부")
                        )
                ));
    }

    private MockHttpSession createAdminSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY, new LoginMember(1L, "admin", MemberRole.ADMIN));
        return session;
    }

    private UniversityDetailResponse createUniversityDetailResponse() {
        return new UniversityDetailResponse(
                1L,
                "클루대학교",
                "cluverse.ac.kr",
                "https://cdn.example.com/universities/clu-badge.png",
                "서울시 관악구 클루대로 1",
                true
        );
    }
}
