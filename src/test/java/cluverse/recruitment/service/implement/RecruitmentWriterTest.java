package cluverse.recruitment.service.implement;

import cluverse.common.exception.ForbiddenException;
import cluverse.group.domain.Group;
import cluverse.group.domain.GroupActivityType;
import cluverse.group.domain.GroupCategory;
import cluverse.group.domain.GroupVisibility;
import cluverse.group.service.implement.GroupReader;
import cluverse.recruitment.domain.Recruitment;
import cluverse.recruitment.repository.RecruitmentRepository;
import cluverse.recruitment.service.request.RecruitmentCreateRequest;
import cluverse.recruitment.service.request.RecruitmentUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecruitmentWriterTest {

    @Mock
    private RecruitmentRepository recruitmentRepository;

    @Mock
    private RecruitmentReader recruitmentReader;

    @Mock
    private GroupReader groupReader;

    @InjectMocks
    private RecruitmentWriter recruitmentWriter;

    @Test
    void 그룹_매니저는_모집글을_생성한다() {
        // given
        Group group = createGroup(1L, 100L);
        RecruitmentCreateRequest request = createRequest();
        when(groupReader.readOrThrow(1L)).thenReturn(group);
        when(recruitmentRepository.save(any(Recruitment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Recruitment result = recruitmentWriter.create(100L, 1L, request);

        // then
        assertThat(result.getGroupId()).isEqualTo(1L);
        assertThat(result.getAuthorId()).isEqualTo(100L);
    }

    @Test
    void 모집글_생성은_그룹_매니저만_가능하다() {
        // given
        Group group = createGroup(1L, 100L);
        when(groupReader.readOrThrow(1L)).thenReturn(group);

        // when, then
        assertThatThrownBy(() -> recruitmentWriter.create(200L, 1L, createRequest()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("모집글 관리 권한이 없습니다.");
    }

    @Test
    void 그룹_매니저는_모집글을_수정한다() {
        // given
        Recruitment recruitment = createRecruitment(10L, 1L, 100L);
        Group group = createGroup(1L, 100L);
        RecruitmentUpdateRequest request = new RecruitmentUpdateRequest(
                "백엔드 모집 수정",
                "설명 수정",
                List.of(),
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 4, 30, 23, 59),
                List.of()
        );
        when(recruitmentReader.readOrThrow(10L)).thenReturn(recruitment);
        when(groupReader.readOrThrow(1L)).thenReturn(group);

        // when
        Recruitment result = recruitmentWriter.update(100L, 10L, request);

        // then
        assertThat(result.getTitle()).isEqualTo("백엔드 모집 수정");
        assertThat(result.getDescription()).isEqualTo("설명 수정");
    }

    @Test
    void 모집글_수정은_그룹_매니저만_가능하다() {
        // given
        Recruitment recruitment = createRecruitment(10L, 1L, 100L);
        Group group = createGroup(1L, 100L);
        when(recruitmentReader.readOrThrow(10L)).thenReturn(recruitment);
        when(groupReader.readOrThrow(1L)).thenReturn(group);
        RecruitmentUpdateRequest request = new RecruitmentUpdateRequest(
                "제목",
                "설명",
                List.of(),
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 4, 30, 23, 59),
                List.of()
        );

        // when, then
        assertThatThrownBy(() -> recruitmentWriter.update(200L, 10L, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("모집글 관리 권한이 없습니다.");
    }

    private RecruitmentCreateRequest createRequest() {
        return new RecruitmentCreateRequest(
                "백엔드 모집",
                "스프링 백엔드 모집 공고",
                List.of(),
                "Spring Boot 경험",
                "3개월",
                "MVP 출시",
                "주 2회 온라인 회의",
                LocalDateTime.of(2026, 3, 31, 23, 59),
                List.of()
        );
    }

    private Group createGroup(Long groupId, Long ownerId) {
        Group group = Group.create(
                11L,
                "AI 프로젝트",
                "설명",
                null,
                GroupCategory.PROJECT,
                GroupActivityType.HYBRID,
                "서울",
                GroupVisibility.PUBLIC,
                ownerId,
                10,
                List.of()
        );
        ReflectionTestUtils.setField(group, "id", groupId);
        return group;
    }

    private Recruitment createRecruitment(Long recruitmentId, Long groupId, Long authorId) {
        Recruitment recruitment = Recruitment.create(
                groupId,
                authorId,
                "백엔드 모집",
                "설명",
                List.of(),
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 3, 31, 23, 59),
                List.of()
        );
        ReflectionTestUtils.setField(recruitment, "id", recruitmentId);
        return recruitment;
    }
}
