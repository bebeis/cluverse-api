package cluverse.group.service.implement;

import cluverse.group.domain.Group;
import cluverse.group.domain.GroupActivityType;
import cluverse.group.domain.GroupCategory;
import cluverse.group.domain.GroupVisibility;
import cluverse.group.repository.GroupRepository;
import cluverse.group.service.request.GroupCreateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupWriterTest {

    @Mock
    private GroupRepository groupRepository;

    @InjectMocks
    private GroupWriter groupWriter;

    @Test
    void 그룹_생성시_기본_직책을_생성하고_오너에게_운영진_직책을_부여한다() {
        // given
        GroupCreateRequest request = new GroupCreateRequest(
                "AI 프로젝트",
                "설명",
                null,
                GroupCategory.PROJECT,
                GroupActivityType.HYBRID,
                "서울",
                GroupVisibility.PUBLIC,
                10,
                List.of(1L, 2L)
        );
        AtomicReference<Group> savedGroup = new AtomicReference<>();
        doAnswer(invocation -> {
            Group group = savedGroup.get();
            long roleId = 1L;
            for (var role : group.getRoles()) {
                ReflectionTestUtils.setField(role, "id", roleId++);
            }
            return null;
        }).when(groupRepository).flush();
        when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> {
            Group group = invocation.getArgument(0);
            savedGroup.set(group);
            return group;
        });

        // when
        Group result = groupWriter.create(100L, 11L, request);

        // then
        assertThat(result.getRoles()).extracting("title").containsExactly("운영진", "멤버");
        assertThat(result.getRoles()).extracting("displayOrder").containsExactly(1, 2);
        assertThat(result.getMember(100L).getCustomTitleId()).isEqualTo(1L);
    }
}
