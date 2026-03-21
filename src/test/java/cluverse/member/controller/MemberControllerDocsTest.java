package cluverse.member.controller;

import cluverse.common.auth.LoginMember;
import cluverse.docs.RestDocsSupport;
import cluverse.member.domain.MajorType;
import cluverse.member.domain.MemberProfileField;
import cluverse.member.domain.MemberRole;
import cluverse.member.domain.VerificationStatus;
import cluverse.member.service.MemberService;
import cluverse.member.service.MemberPostService;
import cluverse.member.service.MemberProfileImageService;
import cluverse.member.service.MemberQueryService;
import cluverse.member.service.MemberUniversityService;
import cluverse.member.service.request.AddInterestRequest;
import cluverse.member.service.request.AddMajorRequest;
import cluverse.member.service.request.MemberNicknameUpdateRequest;
import cluverse.member.service.request.MemberPasswordUpdateRequest;
import cluverse.member.service.request.MemberUniversityUpdateRequest;
import cluverse.member.service.request.UpdateProfileRequest;
import cluverse.member.service.response.BlockedMemberResponse;
import cluverse.member.service.response.MemberFollowResponse;
import cluverse.member.service.response.MemberInterestResponse;
import cluverse.member.service.response.MemberNicknameAvailabilityResponse;
import cluverse.member.service.response.MemberMajorResponse;
import cluverse.member.service.response.MemberProfileImagePresignedUrlResponse;
import cluverse.member.service.response.MemberProfileResponse;
import cluverse.member.service.response.MemberProfileSummaryResponse;
import cluverse.post.domain.PostCategory;
import cluverse.post.service.response.PostAuthorResponse;
import cluverse.post.service.response.PostPageResponse;
import cluverse.post.service.response.PostSummaryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;

import static cluverse.common.auth.LoginMemberArgumentResolver.SESSION_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
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

class MemberControllerDocsTest extends RestDocsSupport {

    private final MemberQueryService memberQueryService = mock(MemberQueryService.class);
    private final MemberService memberService = mock(MemberService.class);
    private final MemberUniversityService memberUniversityService = mock(MemberUniversityService.class);
    private final MemberPostService memberPostService = mock(MemberPostService.class);
    private final MemberProfileImageService memberProfileImageService = mock(MemberProfileImageService.class);

    @Override
    protected Object initController() {
        return new MemberController(memberQueryService, memberService, memberUniversityService, memberPostService, memberProfileImageService);
    }

    @Test
    void 내_프로필_조회() throws Exception {
        when(memberQueryService.getProfile(1L, 1L)).thenReturn(createProfileResponse(1L, true));

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
                                fieldWithPath("data.entranceYear").type(JsonFieldType.NUMBER).description("입학년도").optional(),
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
                                fieldWithPath("data.followingCount").type(JsonFieldType.NUMBER).description("팔로잉 수"),
                                fieldWithPath("data.postCount").type(JsonFieldType.NUMBER).description("작성한 게시글 수")
                        )
                ));
    }

    @Test
    void 상대_프로필_조회() throws Exception {
        when(memberQueryService.getProfile(1L, 2L)).thenReturn(createProfileResponse(2L, false));

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
                                fieldWithPath("data.entranceYear").type(JsonFieldType.NUMBER).description("공개된 입학년도").optional(),
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
                                fieldWithPath("data.followingCount").type(JsonFieldType.NUMBER).description("팔로잉 수"),
                                fieldWithPath("data.postCount").type(JsonFieldType.NUMBER).description("작성한 게시글 수")
                        )
                ));
    }

    @Test
    void 프로필_수정() throws Exception {
        when(memberService.updateProfile(anyLong(), any(UpdateProfileRequest.class)))
                .thenReturn(createProfileResponse(1L, true));

        mockMvc.perform(put("/api/v1/members/me/profile")
                        .session(createSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "bio": "안녕하세요, 클루버스입니다.",
                                    "entranceYear": 2024,
                                    "profileImageUrl": "https://cdn.example.com/profile.png",
                                    "linkGithub": "https://github.com/luna",
                                    "linkNotion": "https://notion.so/luna",
                                    "linkPortfolio": "https://portfolio.example.com",
                                    "linkInstagram": "https://instagram.com/luna",
                                    "linkEtc": "https://blog.example.com",
                                    "isPublic": true,
                                    "visibleFields": ["BIO", "LINK_GITHUB"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(1))
                .andDo(document("members/update-profile",
                        requestFields(
                                fieldWithPath("bio").type(JsonFieldType.STRING).description("자기소개 (최대 500자)").optional(),
                                fieldWithPath("entranceYear").type(JsonFieldType.NUMBER).description("입학년도").optional(),
                                fieldWithPath("profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 URL").optional(),
                                fieldWithPath("linkGithub").type(JsonFieldType.STRING).description("GitHub 링크").optional(),
                                fieldWithPath("linkNotion").type(JsonFieldType.STRING).description("Notion 링크").optional(),
                                fieldWithPath("linkPortfolio").type(JsonFieldType.STRING).description("포트폴리오 링크").optional(),
                                fieldWithPath("linkInstagram").type(JsonFieldType.STRING).description("Instagram 링크").optional(),
                                fieldWithPath("linkEtc").type(JsonFieldType.STRING).description("기타 링크").optional(),
                                fieldWithPath("isPublic").type(JsonFieldType.BOOLEAN).description("프로필 전체 공개 여부"),
                                fieldWithPath("visibleFields").type(JsonFieldType.ARRAY).description("비공개 프로필일 때 외부에 노출할 필드 목록").optional()
                        ),
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
                                fieldWithPath("data.entranceYear").type(JsonFieldType.NUMBER).description("입학년도").optional(),
                                fieldWithPath("data.profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 URL"),
                                fieldWithPath("data.linkGithub").type(JsonFieldType.STRING).description("GitHub 링크"),
                                fieldWithPath("data.linkNotion").type(JsonFieldType.STRING).description("Notion 링크"),
                                fieldWithPath("data.linkPortfolio").type(JsonFieldType.STRING).description("포트폴리오 링크"),
                                fieldWithPath("data.linkInstagram").type(JsonFieldType.STRING).description("Instagram 링크"),
                                fieldWithPath("data.linkEtc").type(JsonFieldType.STRING).description("기타 링크"),
                                fieldWithPath("data.isPublic").type(JsonFieldType.BOOLEAN).description("프로필 전체 공개 여부"),
                                fieldWithPath("data.visibleFields").type(JsonFieldType.ARRAY).description("외부에 노출할 필드 목록"),
                                fieldWithPath("data.isFollowing").type(JsonFieldType.BOOLEAN).description("현재 로그인 사용자의 팔로우 여부"),
                                fieldWithPath("data.isBlocked").type(JsonFieldType.BOOLEAN).description("현재 로그인 사용자의 차단 여부"),
                                fieldWithPath("data.followerCount").type(JsonFieldType.NUMBER).description("팔로워 수"),
                                fieldWithPath("data.followingCount").type(JsonFieldType.NUMBER).description("팔로잉 수"),
                                fieldWithPath("data.postCount").type(JsonFieldType.NUMBER).description("작성한 게시글 수")
                        )
                ));
    }

    @Test
    void 프로필_수정시_isPublic이_null이면_400을_반환한다() throws Exception {
        mockMvc.perform(put("/api/v1/members/me/profile")
                        .session(createSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "bio": "안녕하세요, 클루버스입니다.",
                                    "isPublic": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("프로필 전체 공개 여부를 입력해주세요."));
    }

    @Test
    void 닉네임_중복_확인() throws Exception {
        when(memberQueryService.checkNicknameAvailability("luna"))
                .thenReturn(new MemberNicknameAvailabilityResponse("luna", false));

        mockMvc.perform(get("/api/v1/members/nickname/availability")
                        .queryParam("nickname", "luna"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("luna"))
                .andExpect(jsonPath("$.data.available").value(false))
                .andDo(document("members/check-nickname-availability",
                        queryParameters(
                                parameterWithName("nickname").description("중복 확인할 닉네임")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("확인한 닉네임"),
                                fieldWithPath("data.available").type(JsonFieldType.BOOLEAN).description("사용 가능 여부")
                        )
                ));
    }

    @Test
    void 닉네임_수정() throws Exception {
        when(memberService.updateNickname(anyLong(), any(MemberNicknameUpdateRequest.class)))
                .thenReturn(createProfileResponse(1L, true));

        mockMvc.perform(patch("/api/v1/members/me/nickname")
                        .session(createSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "nickname": "nova"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(1))
                .andDo(document("members/update-nickname",
                        requestFields(
                                fieldWithPath("nickname").type(JsonFieldType.STRING).description("변경할 닉네임")
                        ),
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
                                fieldWithPath("data.entranceYear").type(JsonFieldType.NUMBER).description("입학년도").optional(),
                                fieldWithPath("data.profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 URL"),
                                fieldWithPath("data.linkGithub").type(JsonFieldType.STRING).description("GitHub 링크"),
                                fieldWithPath("data.linkNotion").type(JsonFieldType.STRING).description("Notion 링크"),
                                fieldWithPath("data.linkPortfolio").type(JsonFieldType.STRING).description("포트폴리오 링크"),
                                fieldWithPath("data.linkInstagram").type(JsonFieldType.STRING).description("Instagram 링크"),
                                fieldWithPath("data.linkEtc").type(JsonFieldType.STRING).description("기타 링크"),
                                fieldWithPath("data.isPublic").type(JsonFieldType.BOOLEAN).description("프로필 전체 공개 여부"),
                                fieldWithPath("data.visibleFields").type(JsonFieldType.ARRAY).description("외부에 노출할 필드 목록"),
                                fieldWithPath("data.isFollowing").type(JsonFieldType.BOOLEAN).description("현재 로그인 사용자의 팔로우 여부"),
                                fieldWithPath("data.isBlocked").type(JsonFieldType.BOOLEAN).description("현재 로그인 사용자의 차단 여부"),
                                fieldWithPath("data.followerCount").type(JsonFieldType.NUMBER).description("팔로워 수"),
                                fieldWithPath("data.followingCount").type(JsonFieldType.NUMBER).description("팔로잉 수"),
                                fieldWithPath("data.postCount").type(JsonFieldType.NUMBER).description("작성한 게시글 수")
                        )
                ));
    }

    @Test
    void 학교_수정() throws Exception {
        when(memberUniversityService.updateUniversity(anyLong(), any(MemberUniversityUpdateRequest.class)))
                .thenReturn(createProfileResponse(1L, true));

        mockMvc.perform(put("/api/v1/members/me/university")
                        .session(createSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "universityId": 10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(1))
                .andExpect(jsonPath("$.data.university.universityId").value(10))
                .andDo(document("members/update-university",
                        requestFields(
                                fieldWithPath("universityId").type(JsonFieldType.NUMBER).description("변경할 학교 ID")
                        ),
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
                                fieldWithPath("data.entranceYear").type(JsonFieldType.NUMBER).description("입학년도").optional(),
                                fieldWithPath("data.profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 URL"),
                                fieldWithPath("data.linkGithub").type(JsonFieldType.STRING).description("GitHub 링크"),
                                fieldWithPath("data.linkNotion").type(JsonFieldType.STRING).description("Notion 링크"),
                                fieldWithPath("data.linkPortfolio").type(JsonFieldType.STRING).description("포트폴리오 링크"),
                                fieldWithPath("data.linkInstagram").type(JsonFieldType.STRING).description("Instagram 링크"),
                                fieldWithPath("data.linkEtc").type(JsonFieldType.STRING).description("기타 링크"),
                                fieldWithPath("data.isPublic").type(JsonFieldType.BOOLEAN).description("프로필 전체 공개 여부"),
                                fieldWithPath("data.visibleFields").type(JsonFieldType.ARRAY).description("외부에 노출할 필드 목록"),
                                fieldWithPath("data.isFollowing").type(JsonFieldType.BOOLEAN).description("현재 로그인 사용자의 팔로우 여부"),
                                fieldWithPath("data.isBlocked").type(JsonFieldType.BOOLEAN).description("현재 로그인 사용자의 차단 여부"),
                                fieldWithPath("data.followerCount").type(JsonFieldType.NUMBER).description("팔로워 수"),
                                fieldWithPath("data.followingCount").type(JsonFieldType.NUMBER).description("팔로잉 수"),
                                fieldWithPath("data.postCount").type(JsonFieldType.NUMBER).description("작성한 게시글 수")
                        )
                ));
    }

    @Test
    void 내_게시글_목록_조회() throws Exception {
        when(memberPostService.getMyPosts(anyLong(), any())).thenReturn(new PostPageResponse(
                List.of(
                        new PostSummaryResponse(
                                10L,
                                3L,
                                PostCategory.INFORMATION,
                                "스프링 스터디 모집합니다",
                                "주 1회 온라인으로 진행할 예정입니다.",
                                List.of("spring", "backend"),
                                "https://cdn.example.com/posts/10-thumb.png",
                                false,
                                false,
                                true,
                                120L,
                                15L,
                                4L,
                                8L,
                                new PostAuthorResponse(1L, "luna", "https://cdn.example.com/profile.png"),
                                LocalDateTime.of(2026, 3, 13, 10, 0)
                        )
                ),
                1,
                20,
                true,
                false
        ));

        mockMvc.perform(get("/api/v1/members/me/posts")
                        .session(createSession())
                        .queryParam("page", "1")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.posts[0].postId").value(10))
                .andDo(document("members/get-my-posts",
                        queryParameters(
                                parameterWithName("page").description("페이지 번호").optional(),
                                parameterWithName("size").description("페이지 크기").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.posts").type(JsonFieldType.ARRAY).description("내 게시글 목록"),
                                fieldWithPath("data.posts[].postId").type(JsonFieldType.NUMBER).description("게시글 ID"),
                                fieldWithPath("data.posts[].boardId").type(JsonFieldType.NUMBER).description("게시판 ID"),
                                fieldWithPath("data.posts[].category").type(JsonFieldType.STRING).description("게시글 카테고리"),
                                fieldWithPath("data.posts[].title").type(JsonFieldType.STRING).description("게시글 제목"),
                                fieldWithPath("data.posts[].contentPreview").type(JsonFieldType.STRING).description("게시글 본문 미리보기"),
                                fieldWithPath("data.posts[].tags").type(JsonFieldType.ARRAY).description("태그 목록"),
                                fieldWithPath("data.posts[].thumbnailImageUrl").type(JsonFieldType.STRING).description("썸네일 이미지 URL").optional(),
                                fieldWithPath("data.posts[].isAnonymous").type(JsonFieldType.BOOLEAN).description("익명 여부"),
                                fieldWithPath("data.posts[].isPinned").type(JsonFieldType.BOOLEAN).description("상단 고정 여부"),
                                fieldWithPath("data.posts[].isExternalVisible").type(JsonFieldType.BOOLEAN).description("외부 공개 여부"),
                                fieldWithPath("data.posts[].viewCount").type(JsonFieldType.NUMBER).description("조회수"),
                                fieldWithPath("data.posts[].likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
                                fieldWithPath("data.posts[].commentCount").type(JsonFieldType.NUMBER).description("댓글 수"),
                                fieldWithPath("data.posts[].bookmarkCount").type(JsonFieldType.NUMBER).description("북마크 수"),
                                fieldWithPath("data.posts[].author.memberId").type(JsonFieldType.NUMBER).description("작성자 회원 ID"),
                                fieldWithPath("data.posts[].author.nickname").type(JsonFieldType.STRING).description("작성자 닉네임"),
                                fieldWithPath("data.posts[].author.profileImageUrl").type(JsonFieldType.STRING).description("작성자 프로필 이미지 URL").optional(),
                                fieldWithPath("data.posts[].createdAt").type(JsonFieldType.STRING).description("작성 시각"),
                                fieldWithPath("data.page").type(JsonFieldType.NUMBER).description("현재 페이지"),
                                fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부"),
                                fieldWithPath("data.dateBased").type(JsonFieldType.BOOLEAN).description("날짜 기반 조회 여부 (false)")
                        )
                ));
    }

    @Test
    void 비밀번호_수정() throws Exception {
        doNothing().when(memberService).updatePassword(anyLong(), any(MemberPasswordUpdateRequest.class));

        mockMvc.perform(patch("/api/v1/members/me/password")
                        .session(createSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "currentPassword": "old-password",
                                    "newPassword": "new-password"
                                }
                                """))
                .andExpect(status().isOk())
                .andDo(document("members/update-password",
                        requestFields(
                                fieldWithPath("currentPassword").type(JsonFieldType.STRING).description("현재 비밀번호"),
                                fieldWithPath("newPassword").type(JsonFieldType.STRING).description("새 비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("응답 데이터 없음")
                        )
                ));
    }

    @Test
    void 프로필_이미지_presigned_url_발급() throws Exception {
        when(memberProfileImageService.createPresignedUrl(anyLong(), any())).thenReturn(
                new MemberProfileImagePresignedUrlResponse(
                        "members/1/profile/2026/03/20/test.png",
                        "https://upload.example.com/profile.png",
                        "https://cdn.example.com/profile.png",
                        LocalDateTime.of(2026, 3, 20, 12, 10)
                )
        );

        mockMvc.perform(post("/api/v1/members/me/profile-image/presigned-url")
                        .session(createSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "originalFileName": "profile.png",
                                    "contentType": "image/png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileKey").exists())
                .andDo(document("members/create-profile-image-presigned-url",
                        requestFields(
                                fieldWithPath("originalFileName").type(JsonFieldType.STRING).description("원본 파일명"),
                                fieldWithPath("contentType").type(JsonFieldType.STRING).description("이미지 콘텐츠 타입")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.fileKey").type(JsonFieldType.STRING).description("업로드할 파일 키"),
                                fieldWithPath("data.uploadUrl").type(JsonFieldType.STRING).description("Presigned 업로드 URL"),
                                fieldWithPath("data.imageUrl").type(JsonFieldType.STRING).description("업로드 후 접근 가능한 이미지 URL"),
                                fieldWithPath("data.expiresAt").type(JsonFieldType.STRING).description("업로드 URL 만료 시각")
                        )
                ));
    }

    @Test
    void 회원_탈퇴() throws Exception {
        doNothing().when(memberService).deleteMember(1L);

        mockMvc.perform(delete("/api/v1/members/me")
                        .session(createSession()))
                .andExpect(status().isOk())
                .andDo(document("members/delete-member",
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("응답 데이터 없음")
                        )
                ));
    }

    @Test
    void 내_학과_목록_조회() throws Exception {
        when(memberQueryService.getMajors(1L)).thenReturn(List.of(
                new MemberMajorResponse(1L, 100L, MajorType.PRIMARY, "컴퓨터공학과", "공과대학"),
                new MemberMajorResponse(2L, 200L, MajorType.DOUBLE_MAJOR, "전자공학과", "공과대학")
        ));

        mockMvc.perform(get("/api/v1/members/me/majors")
                        .session(createSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].majorId").value(100))
                .andDo(document("members/get-my-majors",
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data[].memberMajorId").type(JsonFieldType.NUMBER).description("회원 학과 매핑 ID"),
                                fieldWithPath("data[].majorId").type(JsonFieldType.NUMBER).description("학과 ID"),
                                fieldWithPath("data[].majorType").type(JsonFieldType.STRING).description("전공 유형 (`PRIMARY`, `DOUBLE_MAJOR`, `MINOR`)"),
                                fieldWithPath("data[].majorName").type(JsonFieldType.STRING).description("전공명"),
                                fieldWithPath("data[].collegeName").type(JsonFieldType.STRING).description("상위 단과대 또는 카테고리명").optional()
                        )
                ));
    }

    @Test
    void 학과_추가() throws Exception {
        when(memberService.addMajor(anyLong(), any(AddMajorRequest.class)))
                .thenReturn(new MemberMajorResponse(3L, 300L, MajorType.MINOR, "수학과", "자연과학대학"));

        mockMvc.perform(post("/api/v1/members/me/majors")
                        .session(createSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "majorId": 300,
                                    "majorType": "MINOR"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.majorId").value(300))
                .andDo(document("members/add-major",
                        requestFields(
                                fieldWithPath("majorId").type(JsonFieldType.NUMBER).description("추가할 학과 ID"),
                                fieldWithPath("majorType").type(JsonFieldType.STRING).description("전공 유형 (`PRIMARY`, `DOUBLE_MAJOR`, `MINOR`)")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.memberMajorId").type(JsonFieldType.NUMBER).description("회원 학과 매핑 ID"),
                                fieldWithPath("data.majorId").type(JsonFieldType.NUMBER).description("추가된 학과 ID"),
                                fieldWithPath("data.majorType").type(JsonFieldType.STRING).description("전공 유형"),
                                fieldWithPath("data.majorName").type(JsonFieldType.STRING).description("전공명"),
                                fieldWithPath("data.collegeName").type(JsonFieldType.STRING).description("상위 단과대 또는 카테고리명").optional()
                        )
                ));
    }

    @Test
    void 학과_삭제() throws Exception {
        doNothing().when(memberService).removeMajor(1L, 1L);

        mockMvc.perform(delete("/api/v1/members/me/majors/{majorId}", 1L)
                        .session(createSession()))
                .andExpect(status().isOk())
                .andDo(document("members/remove-major",
                        pathParameters(
                                parameterWithName("majorId").description("삭제할 회원 학과 매핑 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("응답 데이터 없음")
                        )
                ));
    }

    @Test
    void 차단_목록_조회() throws Exception {
        when(memberQueryService.getBlockedMembers(1L)).thenReturn(List.of(
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
    void 팔로워_목록_조회() throws Exception {
        when(memberQueryService.getFollowers(2L)).thenReturn(List.of(
                new MemberFollowResponse(3L, "nova", "https://cdn.example.com/nova.png")
        ));

        mockMvc.perform(get("/api/v1/members/{memberId}/followers", 2L)
                        .session(createSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].memberId").value(3))
                .andDo(document("members/get-followers",
                        pathParameters(
                                parameterWithName("memberId").description("조회할 회원 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data[].memberId").type(JsonFieldType.NUMBER).description("팔로워 회원 ID"),
                                fieldWithPath("data[].nickname").type(JsonFieldType.STRING).description("팔로워 닉네임"),
                                fieldWithPath("data[].profileImageUrl").type(JsonFieldType.STRING).description("팔로워 프로필 이미지 URL").optional()
                        )
                ));
    }

    @Test
    void 팔로잉_목록_조회() throws Exception {
        when(memberQueryService.getFollowings(2L)).thenReturn(List.of(
                new MemberFollowResponse(4L, "sol", "https://cdn.example.com/sol.png")
        ));

        mockMvc.perform(get("/api/v1/members/{memberId}/following", 2L)
                        .session(createSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].memberId").value(4))
                .andDo(document("members/get-followings",
                        pathParameters(
                                parameterWithName("memberId").description("조회할 회원 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data[].memberId").type(JsonFieldType.NUMBER).description("팔로잉 회원 ID"),
                                fieldWithPath("data[].nickname").type(JsonFieldType.STRING).description("팔로잉 닉네임"),
                                fieldWithPath("data[].profileImageUrl").type(JsonFieldType.STRING).description("팔로잉 프로필 이미지 URL").optional()
                        )
                ));
    }

    @Test
    void 내_관심사_목록_조회() throws Exception {
        when(memberQueryService.getInterests(1L)).thenReturn(List.of(
                new MemberInterestResponse(100L, "해커톤", "TECH"),
                new MemberInterestResponse(200L, "축제", "CAMPUS")
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
                                fieldWithPath("data[].interestId").type(JsonFieldType.NUMBER).description("관심 태그 ID"),
                                fieldWithPath("data[].interestName").type(JsonFieldType.STRING).description("관심사명"),
                                fieldWithPath("data[].category").type(JsonFieldType.STRING).description("관심사 카테고리").optional()
                        )
                ));
    }

    @Test
    void 관심사_추가() throws Exception {
        when(memberService.addInterest(1L, new AddInterestRequest(300L)))
                .thenReturn(new MemberInterestResponse(300L, "스터디", "ACADEMIC"));

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
                                fieldWithPath("data.interestId").type(JsonFieldType.NUMBER).description("추가된 관심 태그 ID"),
                                fieldWithPath("data.interestName").type(JsonFieldType.STRING).description("관심사명"),
                                fieldWithPath("data.category").type(JsonFieldType.STRING).description("관심사 카테고리").optional()
                        )
                ));
    }

    @Test
    void 관심사_삭제() throws Exception {
        doNothing().when(memberService).removeInterest(1L, 100L);

        mockMvc.perform(delete("/api/v1/members/me/interests/{interestId}", 100L)
                        .session(createSession()))
                .andExpect(status().isOk())
                .andDo(document("members/remove-interest",
                        pathParameters(
                                parameterWithName("interestId").description("삭제할 관심 태그 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("응답 데이터 없음")
                        )
                ));
    }

    @Test
    void 팔로우() throws Exception {
        doNothing().when(memberService).follow(1L, 2L);

        mockMvc.perform(post("/api/v1/members/{memberId}/follow", 2L)
                        .session(createSession()))
                .andExpect(status().isCreated())
                .andDo(document("members/follow",
                        pathParameters(
                                parameterWithName("memberId").description("팔로우할 회원 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("응답 데이터 없음")
                        )
                ));
    }

    @Test
    void 언팔로우() throws Exception {
        doNothing().when(memberService).unfollow(1L, 2L);

        mockMvc.perform(delete("/api/v1/members/{memberId}/follow", 2L)
                        .session(createSession()))
                .andExpect(status().isOk())
                .andDo(document("members/unfollow",
                        pathParameters(
                                parameterWithName("memberId").description("언팔로우할 회원 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("응답 데이터 없음")
                        )
                ));
    }

    @Test
    void 차단() throws Exception {
        doNothing().when(memberService).block(1L, 2L);

        mockMvc.perform(post("/api/v1/members/{memberId}/block", 2L)
                        .session(createSession()))
                .andExpect(status().isCreated())
                .andDo(document("members/block",
                        pathParameters(
                                parameterWithName("memberId").description("차단할 회원 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("응답 데이터 없음")
                        )
                ));
    }

    @Test
    void 차단_해제() throws Exception {
        doNothing().when(memberService).unblock(1L, 2L);

        mockMvc.perform(delete("/api/v1/members/{memberId}/block", 2L)
                        .session(createSession()))
                .andExpect(status().isOk())
                .andDo(document("members/unblock",
                        pathParameters(
                                parameterWithName("memberId").description("차단을 해제할 회원 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("응답 데이터 없음")
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
                2024,
                isMe ? "https://cdn.example.com/profile.png" : null,
                isMe ? "https://github.com/luna" : null,
                isMe ? "https://notion.so/luna" : null,
                isMe ? "https://portfolio.example.com" : null,
                isMe ? "https://instagram.com/luna" : null,
                isMe ? "https://blog.example.com" : null,
                isMe,
                isMe
                        ? List.of(MemberProfileField.BIO, MemberProfileField.ENTRANCE_YEAR, MemberProfileField.LINK_GITHUB)
                        : List.of(MemberProfileField.UNIVERSITY, MemberProfileField.ENTRANCE_YEAR, MemberProfileField.BIO),
                !isMe,
                false,
                12L,
                7L,
                34L
        );
    }
}
