package cluverse.interest.service;

import cluverse.interest.service.implement.InterestReader;
import cluverse.interest.service.response.InterestResponse;
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
class InterestServiceTest {

    @Mock
    private InterestReader interestReader;

    @InjectMocks
    private InterestQueryService interestQueryService;

    @Test
    void 관심사_조회는_reader에_위임한다() {
        // given
        List<InterestResponse> responses = List.of(
                new InterestResponse(1L, 101L, "인공지능", "TECH", null, 1)
        );
        when(interestReader.readInterests()).thenReturn(responses);

        // when
        List<InterestResponse> result = interestQueryService.getInterests();

        // then
        assertThat(result).isEqualTo(responses);
        verify(interestReader).readInterests();
    }
}
