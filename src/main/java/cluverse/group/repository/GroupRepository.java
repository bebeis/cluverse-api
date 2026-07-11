package cluverse.group.repository;

import cluverse.group.domain.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query("""
            select distinct g
            from StudyGroup g
            join g.members member
            where member.memberId = :memberId
            order by g.createdAt desc
            """)
    List<Group> findAllByMemberId(Long memberId);

    @Query("SELECT g FROM StudyGroup g LEFT JOIN FETCH g.members WHERE g.id = :groupId")
    Optional<Group> findWithMembersById(@Param("groupId") Long groupId);
}
