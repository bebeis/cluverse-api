package cluverse.group.repository;

import cluverse.group.domain.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query("""
            select distinct g
            from StudyGroup g
            join g.members member
            where member.memberId = :memberId
            order by g.createdAt desc
            """)
    List<Group> findAllByMemberId(Long memberId);
}
