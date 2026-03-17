package cluverse.major.service.implement;

import cluverse.major.domain.Major;
import cluverse.major.repository.MajorRepository;
import cluverse.major.service.response.MajorResponse;
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
class MajorReaderTest {

    @Mock
    private MajorRepository majorRepository;

    @InjectMocks
    private MajorReader majorReader;

    @Test
    void 부모_id가_없으면_최상위_전공을_조회한다() {
        // given
        when(majorRepository.findAllByIsActiveTrueAndParentIdIsNullOrderByDisplayOrderAscNameAsc()).thenReturn(List.of(
                createMajor(1L, 201L, "컴퓨터공학", null, 0, 1)
        ));

        // when
        List<MajorResponse> result = majorReader.readMajors(null);

        // then
        assertThat(result).containsExactly(
                new MajorResponse(1L, 201L, "컴퓨터공학", null, 0, 1)
        );
        verify(majorRepository).findAllByIsActiveTrueAndParentIdIsNullOrderByDisplayOrderAscNameAsc();
    }

    @Test
    void 부모_id가_있으면_하위_전공을_조회한다() {
        // given
        when(majorRepository.findAllByIsActiveTrueAndParentIdOrderByDisplayOrderAscNameAsc(1L)).thenReturn(List.of(
                createMajor(2L, 202L, "인공지능", 1L, 1, 1)
        ));

        // when
        List<MajorResponse> result = majorReader.readMajors(1L);

        // then
        assertThat(result).containsExactly(
                new MajorResponse(2L, 202L, "인공지능", 1L, 1, 1)
        );
        verify(majorRepository).findAllByIsActiveTrueAndParentIdOrderByDisplayOrderAscNameAsc(1L);
    }

    private Major createMajor(
            Long majorId,
            Long boardId,
            String name,
            Long parentId,
            int depth,
            int displayOrder
    ) {
        Major major = BeanUtils.instantiateClass(Major.class);
        ReflectionTestUtils.setField(major, "id", majorId);
        ReflectionTestUtils.setField(major, "boardId", boardId);
        ReflectionTestUtils.setField(major, "name", name);
        ReflectionTestUtils.setField(major, "parentId", parentId);
        ReflectionTestUtils.setField(major, "depth", depth);
        ReflectionTestUtils.setField(major, "displayOrder", displayOrder);
        return major;
    }
}
