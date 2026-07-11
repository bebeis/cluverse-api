package cluverse.member.repository;

import cluverse.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

    boolean existsByNickname(String nickname);

    @Query("SELECT member FROM Member member LEFT JOIN FETCH member.profile WHERE member.id = :memberId")
    Optional<Member> findWithProfileById(@Param("memberId") Long memberId);

    @Query("SELECT member FROM Member member LEFT JOIN FETCH member.profile WHERE member.id IN :memberIds")
    List<Member> findAllWithProfileByIdIn(@Param("memberIds") Collection<Long> memberIds);
}
