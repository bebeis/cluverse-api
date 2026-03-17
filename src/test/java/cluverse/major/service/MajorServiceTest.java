package cluverse.major.service;

import cluverse.major.service.implement.MajorReader;
import cluverse.major.service.response.MajorResponse;
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
class MajorServiceTest {

    @Mock
    private MajorReader majorReader;

    @InjectMocks
    private MajorService majorService;

    @Test
    void 전공_조회는_reader에_위임한다() {
        // given
        List<MajorResponse> responses = List.of(
                new MajorResponse(10L, 210L, "컴퓨터공학", null, 0, 1)
        );
        when(majorReader.readMajors(null)).thenReturn(responses);

        // when
        List<MajorResponse> result = majorService.getMajors(null);

        // then
        assertThat(result).isEqualTo(responses);
        verify(majorReader).readMajors(null);
    }
}
