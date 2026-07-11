package cluverse.recruitment.service;

import cluverse.recruitment.service.implement.RecruitmentApplicationProcessor;
import cluverse.recruitment.service.request.ApplicationChatMessageCreateRequest;
import cluverse.recruitment.service.request.RecruitmentApplicationCreateRequest;
import cluverse.recruitment.service.request.RecruitmentApplicationStatusUpdateRequest;
import cluverse.recruitment.domain.RecruitmentApplicationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecruitmentApplicationServiceTest {

    @Mock
    private RecruitmentApplicationProcessor recruitmentApplicationProcessor;

    @InjectMocks
    private RecruitmentApplicationService recruitmentApplicationService;

    @Test
    void 지원_생성은_Processor에_위임한다() {
        RecruitmentApplicationCreateRequest request = new RecruitmentApplicationCreateRequest(
                "Backend",
                null,
                List.of()
        );
        when(recruitmentApplicationProcessor.createApplication(200L, 10L, request, "127.0.0.1")).thenReturn(30L);

        Long result = recruitmentApplicationService.createApplication(200L, 10L, request, "127.0.0.1");

        assertThat(result).isEqualTo(30L);
        verify(recruitmentApplicationProcessor).createApplication(200L, 10L, request, "127.0.0.1");
    }

    @Test
    void 지원_상태변경은_Processor에_위임한다() {
        RecruitmentApplicationStatusUpdateRequest request = new RecruitmentApplicationStatusUpdateRequest(
                RecruitmentApplicationStatus.APPROVED,
                "합류 승인"
        );
        when(recruitmentApplicationProcessor.updateApplicationStatus(100L, 30L, request, "127.0.0.1")).thenReturn(30L);

        Long result = recruitmentApplicationService.updateApplicationStatus(100L, 30L, request, "127.0.0.1");

        assertThat(result).isEqualTo(30L);
        verify(recruitmentApplicationProcessor).updateApplicationStatus(100L, 30L, request, "127.0.0.1");
    }

    @Test
    void 지원_취소는_Processor에_위임한다() {
        recruitmentApplicationService.cancelApplication(200L, 30L, "127.0.0.1");

        verify(recruitmentApplicationProcessor).cancelApplication(200L, 30L, "127.0.0.1");
    }

    @Test
    void 지원_메시지_작성은_Processor에_위임한다() {
        ApplicationChatMessageCreateRequest request = new ApplicationChatMessageCreateRequest("안녕하세요");
        when(recruitmentApplicationProcessor.createMessage(200L, 30L, request, "127.0.0.1")).thenReturn(5L);

        Long result = recruitmentApplicationService.createMessage(200L, 30L, request, "127.0.0.1");

        assertThat(result).isEqualTo(5L);
        verify(recruitmentApplicationProcessor).createMessage(200L, 30L, request, "127.0.0.1");
    }
}
