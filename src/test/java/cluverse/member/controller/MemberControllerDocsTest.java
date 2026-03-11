package cluverse.member.controller;

import cluverse.common.auth.LoginMember;
import cluverse.docs.RestDocsSupport;
import cluverse.member.domain.MemberProfileField;
import cluverse.member.domain.MemberRole;
import cluverse.member.domain.VerificationStatus;
import cluverse.member.service.MemberService;
import cluverse.member.service.response.BlockedMemberResponse;
import cluverse.member.service.response.MemberInterestResponse;
import cluverse.member.service.response.MemberProfileResponse;
import cluverse.member.service.response.MemberProfileSummaryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;

import static cluverse.common.auth.LoginMemberArgumentResolver.SESSION_KEY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemberControllerDocsTest extends RestDocsSupport {

    private final MemberService memberService = mock(MemberService.class);

    @Override
    protected Object initController() {
        return new MemberController(memberService);
    }

    @Test
    void 내_프로필_조회() throws Exception {
        when(memberService.getProfile(1L, 1L)).thenReturn(createProfileResponse(1L, true));

        mockMvc.perform(get("/api/v1/members/me/profile")
                        .session(createSession())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(1))
                .andExpect(jsonPath("$.data.university.universityName").value("클루대"))
                .andDo(document("members/get-my-profile",
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.memberId").type(JsonFieldType.NUMBER).description("회원 ID"),
                                fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("닉네임"),
                                fieldWithPath("data.university.universityId").type(JsonFieldType.NUMBER).description("학교 ID"),
                                fieldWithPath("data.university.universityName").type(JsonFieldType.STRING).description("학교명"),
                                fieldWithPath("data.university.universityBadgeImageUrl").type(JsonFieldType.STRING).description("학교 배지 이미지 URL"),
                                fieldWithPath("data.verificationStatus").type(JsonFieldType.STRING).description("학생 인증 상태"),
                                fieldWithPath("data.bio").type(JsonFieldType.STRING).description("자기소개"),
                                fieldWithPath("data.profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 URL"),
                                fieldWithPath("data.linkGithub").type(JsonFieldType.STRING).description("GitHub 링크"),
                                fieldWithPath("data.linkNotion").type(JsonFieldType.STRING).description("Notion 링크"),
                                fieldWithPath("data.linkPortfolio").type(JsonFieldType.STRING).description("포트폴리오 링크"),
                                fieldWithPath("data.linkInstagram").type(JsonFieldType.STRING).description("Instagram 링크"),
                                fieldWithPath("data.linkEtc").type(JsonFieldType.STRING).description("기타 링크"),
                                fieldWithPath("data.isPublic").type(JsonFieldType.BOOLEAN).description("프로필 전체 공개 여부"),
                                fieldWithPath("data.visibleFields").type(JsonFieldType.ARRAY).description("비공개 프로필일 때 외부에 노출할 필드 목록"),
                                fieldWithPath("data.isFollowing").type(JsonFieldType.BOOLEAN).description("현재 로그인 사용자의 팔로우 여부"),
                                fieldWithPath("data.isBlocked").type(JsonFieldType.BOOLEAN).description("현재 로그인 사용자의 차단 여부"),
                                fieldWithPath("data.followerCount").type(JsonFieldType.NUMBER).description("팔로워 수"),
                                fieldWithPath("data.followingCount").type(JsonFieldType.NUMBER).description("팔로잉 수")
                        )
                ));
    }

    @Test
    void 상대_프로필_조회() throws Exception {
        when(memberService.getProfile(1L, 2L)).thenReturn(createProfileResponse(2L, false));

        mockMvc.perform(get("/api/v1/members/{memberId}/profile", 2L)
                        .session(createSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(2))
                .andExpect(jsonPath("$.data.isFollowing").value(true))
                .andDo(document("members/get-profile",
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.memberId").type(JsonFieldType.NUMBER).description("회원 ID"),
                                fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("닉네임"),
                                fieldWithPath("data.university.universityId").type(JsonFieldType.NUMBER).description("학교 ID"),
                                fieldWithPath("data.university.universityName").type(JsonFieldType.STRING).description("학교명"),
                                fieldWithPath("data.university.universityBadgeImageUrl").type(JsonFieldType.STRING).description("학교 배지 이미지 URL"),
                                fieldWithPath("data.verificationStatus").type(JsonFieldType.STRING).description("학생 인증 상태"),
                                fieldWithPath("data.bio").type(JsonFieldType.STRING).description("공개된 자기소개"),
                                fieldWithPath("data.profileImageUrl").type(JsonFieldType.NULL).description("공개되지 않은 프로필 이미지 URL"),
                                fieldWithPath("data.linkGithub").type(JsonFieldType.NULL).description("공개되지 않은 GitHub 링크"),
                                fieldWithPath("data.linkNotion").type(JsonFieldType.NULL).description("공개되지 않은 Notion 링크"),
                                fieldWithPath("data.linkPortfolio").type(JsonFieldType.NULL).description("공개되지 않은 포트폴리오 링크"),
                                fieldWithPath("data.linkInstagram").type(JsonFieldType.NULL).description("공개되지 않은 Instagram 링크"),
                                fieldWithPath("data.linkEtc").type(JsonFieldType.NULL).description("공개되지 않은 기타 링크"),
                                fieldWithPath("data.isPublic").type(JsonFieldType.BOOLEAN).description("프로필 전체 공개 여부"),
                                fieldWithPath("data.visibleFields").type(JsonFieldType.ARRAY).description("외부에 노출된 필드 목록"),
                                fieldWithPath("data.isFollowing").type(JsonFieldType.BOOLEAN).description("현재 로그인 사용자의 팔로우 여부"),
                                fieldWithPath("data.isBlocked").type(JsonFieldType.BOOLEAN).description("현재 로그인 사용자의 차단 여부"),
                                fieldWithPath("data.followerCount").type(JsonFieldType.NUMBER).description("팔로워 수"),
                                fieldWithPath("data.followingCount").type(JsonFieldType.NUMBER).description("팔로잉 수")
                        )
                ));
    }

    @Test
    void 차단_목록_조회() throws Exception {
        when(memberService.getBlockedMembers(1L)).thenReturn(List.of(
                new BlockedMemberResponse(
                        2L,
                        "blocked-user",
                        10L,
                        "클루대",
                        "https://cdn.example.com/badge.png",
                        "https://cdn.example.com/profile.png",
                        LocalDateTime.of(2026, 3, 12, 10, 0)
                )
        ));

        mockMvc.perform(get("/api/v1/members/me/blocks")
                        .session(createSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].memberId").value(2))
                .andDo(document("members/get-my-blocks",
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data[].memberId").type(JsonFieldType.NUMBER).description("차단한 회원 ID"),
                                fieldWithPath("data[].nickname").type(JsonFieldType.STRING).description("차단한 회원 닉네임"),
                                fieldWithPath("data[].universityId").type(JsonFieldType.NUMBER).description("차단한 회원 학교 ID"),
                                fieldWithPath("data[].universityName").type(JsonFieldType.STRING).description("차단한 회원 학교명"),
                                fieldWithPath("data[].universityBadgeImageUrl").type(JsonFieldType.STRING).description("차단한 회원 학교 배지 이미지 URL"),
                                fieldWithPath("data[].profileImageUrl").type(JsonFieldType.STRING).description("차단한 회원 프로필 이미지 URL"),
                                fieldWithPath("data[].blockedAt").type(JsonFieldType.STRING).description("차단 시각")
                        )
                ));
    }

    @Test
    void 내_관심사_목록_조회() throws Exception {
        when(memberService.getInterests(1L)).thenReturn(List.of(
                new MemberInterestResponse(100L),
                new MemberInterestResponse(200L)
        ));

        mockMvc.perform(get("/api/v1/members/me/interests")
                        .session(createSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].interestId").value(100))
                .andDo(document("members/get-my-interests",
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data[].interestId").type(JsonFieldType.NUMBER).description("관심 태그 ID")
                        )
                ));
    }

    @Test
    void 관심사_추가() throws Exception {
        when(memberService.addInterest(1L, new cluverse.member.service.request.AddInterestRequest(300L)))
                .thenReturn(new MemberInterestResponse(300L));

        mockMvc.perform(post("/api/v1/members/me/interests")
                        .session(createSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "interestId": 300
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.interestId").value(300))
                .andDo(document("members/add-interest",
                        requestFields(
                                fieldWithPath("interestId").type(JsonFieldType.NUMBER).description("추가할 관심 태그 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.interestId").type(JsonFieldType.NUMBER).description("추가된 관심 태그 ID")
                        )
                ));
    }

    private MockHttpSession createSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY, new LoginMember(1L, "luna", MemberRole.MEMBER));
        return session;
    }

    private MemberProfileResponse createProfileResponse(Long memberId, boolean isMe) {
        return new MemberProfileResponse(
                memberId,
                "luna",
                new MemberProfileSummaryResponse(10L, "클루대", "https://cdn.example.com/badge.png"),
                VerificationStatus.APPROVED,
                "소개",
                isMe ? "https://cdn.example.com/profile.png" : null,
                isMe ? "https://github.com/luna" : null,
                isMe ? "https://notion.so/luna" : null,
                isMe ? "https://portfolio.example.com" : null,
                isMe ? "https://instagram.com/luna" : null,
                isMe ? "https://blog.example.com" : null,
                isMe,
                isMe ? List.of(MemberProfileField.BIO, MemberProfileField.LINK_GITHUB) : List.of(MemberProfileField.UNIVERSITY, MemberProfileField.BIO),
                !isMe,
                false,
                12L,
                7L
        );
    }
}
