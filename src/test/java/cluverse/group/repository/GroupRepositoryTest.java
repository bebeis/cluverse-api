package cluverse.group.repository;

import cluverse.group.domain.Group;
import cluverse.group.domain.GroupActivityType;
import cluverse.group.domain.GroupCategory;
import cluverse.group.domain.GroupVisibility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class GroupRepositoryTest {

    @Autowired
    private GroupRepository groupRepository;

    @Test
    void 그룹_상세_조회시_멤버_직책_관심태그를_함께_가져온다() {
        // given
        Group group = Group.create(
                11L,
                "AI 프로젝트",
                "설명",
                null,
                GroupCategory.PROJECT,
                GroupActivityType.HYBRID,
                "서울",
                GroupVisibility.PUBLIC,
                100L,
                10,
                List.of(1L, 2L)
        );
        group.addRole("운영진", 1);
        group.addMember(200L);
        Group saved = groupRepository.save(group);

        // when
        Group result = groupRepository.findById(saved.getId()).orElseThrow();

        // then
        assertThat(result.getMembers()).hasSize(2);
        assertThat(result.getRoles()).hasSize(1);
        assertThat(result.getInterests()).hasSize(2);
    }

    @Test
    void 특정_회원이_속한_그룹을_조회한다() {
        // given
        Group first = groupRepository.save(Group.create(
                11L,
                "AI 프로젝트",
                "설명",
                null,
                GroupCategory.PROJECT,
                GroupActivityType.HYBRID,
                "서울",
                GroupVisibility.PUBLIC,
                100L,
                10,
                List.of()
        ));
        first.addMember(200L);

        groupRepository.save(Group.create(
                12L,
                "스터디",
                "설명",
                null,
                GroupCategory.STUDY,
                GroupActivityType.ONLINE,
                null,
                GroupVisibility.PUBLIC,
                300L,
                10,
                List.of()
        ));
        groupRepository.flush();

        // when
        List<Group> result = groupRepository.findAllByMemberId(200L);

        // then
        assertThat(result).extracting(Group::getId).contains(first.getId());
    }
}
