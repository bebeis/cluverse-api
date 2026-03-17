package cluverse.member.service.implement;

import cluverse.member.domain.Terms;
import cluverse.member.repository.TermsRepository;
import cluverse.member.service.response.TermsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TermsReaderTest {

    @Mock
    private TermsRepository termsRepository;

    @InjectMocks
    private TermsReader termsReader;

    @Test
    void 활성_약관_목록을_조회한다() {
        // given
        when(termsRepository.findAllByIsActiveTrueOrderByIsRequiredDescIdAsc()).thenReturn(List.of(
                createTerms(1L, "SERVICE", "서비스 이용약관", "약관 내용", "1.0.0", true, LocalDateTime.of(2026, 3, 1, 0, 0))
        ));

        // when
        List<TermsResponse> result = termsReader.readTerms();

        // then
        assertThat(result).containsExactly(
                new TermsResponse(1L, "SERVICE", "서비스 이용약관", "약관 내용", "1.0.0", true, LocalDateTime.of(2026, 3, 1, 0, 0))
        );
        verify(termsRepository).findAllByIsActiveTrueOrderByIsRequiredDescIdAsc();
    }

    private Terms createTerms(
            Long termsId,
            String termsType,
            String title,
            String content,
            String version,
            boolean required,
            LocalDateTime effectiveAt
    ) {
        Terms terms = BeanUtils.instantiateClass(Terms.class);
        ReflectionTestUtils.setField(terms, "id", termsId);
        ReflectionTestUtils.setField(terms, "termsType", termsType);
        ReflectionTestUtils.setField(terms, "title", title);
        ReflectionTestUtils.setField(terms, "content", content);
        ReflectionTestUtils.setField(terms, "version", version);
        ReflectionTestUtils.setField(terms, "isRequired", required);
        ReflectionTestUtils.setField(terms, "effectiveAt", effectiveAt);
        return terms;
    }
}
