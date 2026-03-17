package cluverse.interest.service.implement;

import cluverse.interest.domain.Interest;
import cluverse.interest.repository.InterestRepository;
import cluverse.interest.service.response.InterestResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterestReaderTest {

    @Mock
    private InterestRepository interestRepository;

    @InjectMocks
    private InterestReader interestReader;

    @Test
    void 활성_관심사_목록을_조회한다() {
        // given
        when(interestRepository.findAllByIsActiveTrueOrderByDisplayOrderAscNameAsc()).thenReturn(List.of(
                createInterest(1L, 101L, "인공지능", "TECH", null, 1),
                createInterest(2L, 102L, "백엔드", "TECH", 1L, 2)
        ));

        // when
        List<InterestResponse> result = interestReader.readInterests();

        // then
        assertThat(result).containsExactly(
                new InterestResponse(1L, 101L, "인공지능", "TECH", null, 1),
                new InterestResponse(2L, 102L, "백엔드", "TECH", 1L, 2)
        );
        verify(interestRepository).findAllByIsActiveTrueOrderByDisplayOrderAscNameAsc();
    }

    private Interest createInterest(
            Long interestId,
            Long boardId,
            String name,
            String category,
            Long parentId,
            int displayOrder
    ) {
        Interest interest = BeanUtils.instantiateClass(Interest.class);
        ReflectionTestUtils.setField(interest, "id", interestId);
        ReflectionTestUtils.setField(interest, "boardId", boardId);
        ReflectionTestUtils.setField(interest, "name", name);
        ReflectionTestUtils.setField(interest, "category", category);
        ReflectionTestUtils.setField(interest, "parentId", parentId);
        ReflectionTestUtils.setField(interest, "displayOrder", displayOrder);
        return interest;
    }
}
