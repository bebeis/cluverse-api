package cluverse.member.service;

import cluverse.member.service.implement.TermsReader;
import cluverse.member.service.response.TermsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TermsServiceTest {

    @Mock
    private TermsReader termsReader;

    @InjectMocks
    private TermsQueryService termsQueryService;

    @Test
    void 약관_조회는_reader에_위임한다() {
        // given
        List<TermsResponse> responses = List.of(
                new TermsResponse(1L, "SERVICE", "서비스 이용약관", "내용", "1.0.0", true, LocalDateTime.of(2026, 3, 1, 0, 0))
        );
        when(termsReader.readTerms()).thenReturn(responses);

        // when
        List<TermsResponse> result = termsQueryService.getTerms();

        // then
        assertThat(result).isEqualTo(responses);
        verify(termsReader).readTerms();
    }
}
