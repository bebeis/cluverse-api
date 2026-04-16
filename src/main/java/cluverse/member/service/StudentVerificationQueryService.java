package cluverse.member.service;

import cluverse.member.domain.Member;
import cluverse.member.service.implement.MemberReader;
import cluverse.member.service.implement.StudentVerificationReader;
import cluverse.member.service.response.StudentVerificationStatusResponse;
import cluverse.university.domain.University;
import cluverse.university.service.implement.UniversityReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentVerificationQueryService {

    private final MemberReader memberReader;
    private final StudentVerificationReader studentVerificationReader;
    private final UniversityReader universityReader;

    public StudentVerificationStatusResponse getVerificationStatus(Long memberId) {
        Member member = memberReader.readOrThrow(memberId);
        return studentVerificationReader.findByMemberId(memberId)
                .map(studentVerification -> StudentVerificationStatusResponse.from(
                        studentVerification,
                        readUniversityOrNull(studentVerification.getUniversityId())
                ))
                .orElseGet(() -> StudentVerificationStatusResponse.fromCompatibility(
                        member,
                        readUniversityOrNull(member.getUniversityId())
                ));
    }

    private University readUniversityOrNull(Long universityId) {
        if (universityId == null) {
            return null;
        }

        return universityReader.readOrThrow(universityId);
    }
}
