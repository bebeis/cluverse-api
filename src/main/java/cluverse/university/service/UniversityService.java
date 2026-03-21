package cluverse.university.service;

import cluverse.common.exception.ForbiddenException;
import cluverse.member.service.implement.MemberReader;
import cluverse.university.domain.University;
import cluverse.university.exception.UniversityExceptionMessage;
import cluverse.university.service.implement.UniversityReader;
import cluverse.university.service.implement.UniversityWriter;
import cluverse.university.service.request.UniversityCreateRequest;
import cluverse.university.service.request.UniversityUpdateRequest;
import cluverse.university.service.response.UniversityDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UniversityService {

    private final UniversityReader universityReader;
    private final UniversityWriter universityWriter;
    private final MemberReader memberReader;

    public UniversityDetailResponse createUniversity(Long memberId, UniversityCreateRequest request) {
        validateAdmin(memberId);
        University university = universityWriter.create(request);
        return UniversityDetailResponse.from(university);
    }

    public UniversityDetailResponse updateUniversity(Long memberId, Long universityId, UniversityUpdateRequest request) {
        validateAdmin(memberId);
        University university = universityReader.readOrThrow(universityId);
        universityWriter.update(university, request);
        return UniversityDetailResponse.from(university);
    }

    private void validateAdmin(Long memberId) {
        if (!memberReader.isAdmin(memberId)) {
            throw new ForbiddenException(UniversityExceptionMessage.UNIVERSITY_ACCESS_DENIED.getMessage());
        }
    }
}
