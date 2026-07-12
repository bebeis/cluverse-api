package cluverse.group.service;

import cluverse.group.domain.GroupActivityType;
import cluverse.group.domain.GroupCategory;
import cluverse.group.domain.GroupVisibility;
import cluverse.group.service.implement.GroupProcessor;
import cluverse.group.service.request.GroupCreateRequest;
import cluverse.group.service.request.GroupUpdateRequest;
import cluverse.group.service.response.GroupDetailResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupProcessor groupProcessor;

    @InjectMocks
    private GroupService groupService;

    @Test
    void 그룹_생성은_Processor에_위임한다() {
        GroupCreateRequest request = new GroupCreateRequest(
                "AI 프로젝트",
                "함께 만드는 AI 프로젝트 그룹",
                null,
                GroupCategory.PROJECT,
                GroupActivityType.HYBRID,
                "서울",
                GroupVisibility.PUBLIC,
                10,
                List.of(1L, 2L)
        );
        GroupDetailResponse response = mock(GroupDetailResponse.class);
        when(groupProcessor.createGroup(100L, request)).thenReturn(response);

        GroupDetailResponse result = groupService.createGroup(100L, request);

        assertThat(result).isSameAs(response);
        verify(groupProcessor).createGroup(100L, request);
    }

    @Test
    void 그룹_수정은_Processor에_위임한다() {
        GroupUpdateRequest request = new GroupUpdateRequest(
                "AI 프로젝트 시즌2",
                "업데이트된 소개",
                null,
                GroupCategory.PROJECT,
                GroupActivityType.ONLINE,
                "전국",
                GroupVisibility.PUBLIC,
                20,
                List.of(1L)
        );
        GroupDetailResponse response = mock(GroupDetailResponse.class);
        when(groupProcessor.updateGroup(100L, 1L, request)).thenReturn(response);

        GroupDetailResponse result = groupService.updateGroup(100L, 1L, request);

        assertThat(result).isSameAs(response);
        verify(groupProcessor).updateGroup(100L, 1L, request);
    }

    @Test
    void 그룹_삭제는_Processor에_위임한다() {
        groupService.deleteGroup(100L, 1L);

        verify(groupProcessor).deleteGroup(100L, 1L);
    }
}
