package cluverse.member.repository;

import cluverse.member.domain.StudentVerificationEmailChallenge;
import cluverse.member.domain.StudentVerificationEmailChallengeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface StudentVerificationEmailChallengeRepository
        extends JpaRepository<StudentVerificationEmailChallenge, Long> {

    Optional<StudentVerificationEmailChallenge> findByChallengeId(String challengeId);

    Optional<StudentVerificationEmailChallenge> findFirstByStudentVerificationIdAndStatusOrderByCreatedAtDesc(
            Long studentVerificationId,
            StudentVerificationEmailChallengeStatus status
    );

    @Modifying
    @Query("""
            update StudentVerificationEmailChallenge challenge
            set challenge.status = :replacedStatus
            where challenge.studentVerificationId = :studentVerificationId
              and challenge.status = :pendingStatus
            """)
    int replacePendingChallenges(
            Long studentVerificationId,
            StudentVerificationEmailChallengeStatus pendingStatus,
            StudentVerificationEmailChallengeStatus replacedStatus
    );

    @Modifying
    @Query("""
            update StudentVerificationEmailChallenge challenge
            set challenge.status = :expiredStatus
            where challenge.status = :pendingStatus
              and challenge.expiresAt <= :now
            """)
    int expirePendingChallenges(
            StudentVerificationEmailChallengeStatus pendingStatus,
            StudentVerificationEmailChallengeStatus expiredStatus,
            LocalDateTime now
    );
}
