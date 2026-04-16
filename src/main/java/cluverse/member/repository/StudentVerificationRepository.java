package cluverse.member.repository;

import cluverse.member.domain.StudentVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentVerificationRepository extends JpaRepository<StudentVerification, Long> {

    Optional<StudentVerification> findByMemberId(Long memberId);
}
