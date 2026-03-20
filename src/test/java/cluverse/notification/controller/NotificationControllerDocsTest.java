package cluverse.notification.controller;

import cluverse.common.auth.LoginMember;
import cluverse.docs.RestDocsSupport;
import cluverse.member.domain.MemberRole;
import cluverse.notification.domain.NotificationType;
import cluverse.notification.service.NotificationService;
import cluverse.notification.service.response.NotificationPreferenceResponse;
import cluverse.notification.service.response.NotificationResponse;
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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerDocsTest extends RestDocsSupport {

    private final NotificationService notificationService = mock(NotificationService.class);

    @Override
    protected Object initController() {
        return new NotificationController(notificationService);
    }

    @Test
    void 알림_목록_조회() throws Exception {
        when(notificationService.getNotifications(1L)).thenReturn(List.of(createNotification()));

        mockMvc.perform(get("/api/v1/notifications").session(createSession()))
                .andExpect(status().isOk())
                .andDo(document("notifications/get-list",
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data[].notificationId").type(JsonFieldType.NUMBER).description("알림 ID"),
                                fieldWithPath("data[].type").type(JsonFieldType.STRING).description("알림 타입"),
                                fieldWithPath("data[].title").type(JsonFieldType.STRING).description("제목"),
                                fieldWithPath("data[].content").type(JsonFieldType.STRING).description("본문"),
                                fieldWithPath("data[].excerpt").type(JsonFieldType.STRING).description("요약").optional(),
                                fieldWithPath("data[].isRead").type(JsonFieldType.BOOLEAN).description("읽음 여부"),
                                fieldWithPath("data[].createdAt").type(JsonFieldType.STRING).description("생성 시각"),
                                fieldWithPath("data[].targetUrl").type(JsonFieldType.STRING).description("이동 URL").optional()
                        )
                ));
    }

    @Test
    void 알림_전체_읽음() throws Exception {
        doNothing().when(notificationService).readAll(1L);

        mockMvc.perform(post("/api/v1/notifications/read-all").session(createSession()))
                .andExpect(status().isOk())
                .andDo(document("notifications/read-all",
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("데이터 없음")
                        )
                ));
    }

    @Test
    void 알림_단건_읽음() throws Exception {
        when(notificationService.read(1L, 1L)).thenReturn(createNotification());

        mockMvc.perform(patch("/api/v1/notifications/{notificationId}/read", 1L).session(createSession()))
                .andExpect(status().isOk())
                .andDo(document("notifications/read",
                        pathParameters(
                                parameterWithName("notificationId").description("알림 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.notificationId").type(JsonFieldType.NUMBER).description("알림 ID"),
                                fieldWithPath("data.type").type(JsonFieldType.STRING).description("알림 타입"),
                                fieldWithPath("data.title").type(JsonFieldType.STRING).description("제목"),
                                fieldWithPath("data.content").type(JsonFieldType.STRING).description("본문"),
                                fieldWithPath("data.excerpt").type(JsonFieldType.STRING).description("요약").optional(),
                                fieldWithPath("data.isRead").type(JsonFieldType.BOOLEAN).description("읽음 여부"),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성 시각"),
                                fieldWithPath("data.targetUrl").type(JsonFieldType.STRING).description("이동 URL").optional()
                        )
                ));
    }

    @Test
    void 알림_설정_조회() throws Exception {
        when(notificationService.getPreferences(1L)).thenReturn(new NotificationPreferenceResponse(true, true, true, true, false));

        mockMvc.perform(get("/api/v1/notification-preferences").session(createSession()))
                .andExpect(status().isOk())
                .andDo(document("notifications/get-preferences",
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.comments").type(JsonFieldType.BOOLEAN).description("댓글 알림"),
                                fieldWithPath("data.groups").type(JsonFieldType.BOOLEAN).description("그룹 알림"),
                                fieldWithPath("data.announcements").type(JsonFieldType.BOOLEAN).description("공지 알림"),
                                fieldWithPath("data.follows").type(JsonFieldType.BOOLEAN).description("팔로우 알림"),
                                fieldWithPath("data.marketing").type(JsonFieldType.BOOLEAN).description("마케팅 알림")
                        )
                ));
    }

    @Test
    void 알림_설정_수정() throws Exception {
        when(notificationService.updatePreferences(anyLong(), any()))
                .thenReturn(new NotificationPreferenceResponse(true, true, false, true, false));

        mockMvc.perform(put("/api/v1/notification-preferences")
                        .session(createSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "comments": true,
                                  "groups": true,
                                  "announcements": false,
                                  "follows": true,
                                  "marketing": false
                                }
                                """))
                .andExpect(status().isOk())
                .andDo(document("notifications/update-preferences",
                        requestFields(
                                fieldWithPath("comments").type(JsonFieldType.BOOLEAN).description("댓글 알림"),
                                fieldWithPath("groups").type(JsonFieldType.BOOLEAN).description("그룹 알림"),
                                fieldWithPath("announcements").type(JsonFieldType.BOOLEAN).description("공지 알림"),
                                fieldWithPath("follows").type(JsonFieldType.BOOLEAN).description("팔로우 알림"),
                                fieldWithPath("marketing").type(JsonFieldType.BOOLEAN).description("마케팅 알림")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.comments").type(JsonFieldType.BOOLEAN).description("댓글 알림"),
                                fieldWithPath("data.groups").type(JsonFieldType.BOOLEAN).description("그룹 알림"),
                                fieldWithPath("data.announcements").type(JsonFieldType.BOOLEAN).description("공지 알림"),
                                fieldWithPath("data.follows").type(JsonFieldType.BOOLEAN).description("팔로우 알림"),
                                fieldWithPath("data.marketing").type(JsonFieldType.BOOLEAN).description("마케팅 알림")
                        )
                ));
    }

    private MockHttpSession createSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY, new LoginMember(1L, "luna", MemberRole.MEMBER));
        return session;
    }

    private NotificationResponse createNotification() {
        return new NotificationResponse(
                1L,
                NotificationType.COMMENT,
                "새 댓글이 달렸습니다",
                "내 게시글에 댓글이 달렸습니다.",
                "좋은 글 잘 봤어요!",
                false,
                LocalDateTime.of(2026, 3, 20, 15, 0),
                "/post/10"
        );
    }
}
