package cluverse.university.service.implement;

import cluverse.common.exception.NotFoundException;
import cluverse.university.domain.University;
import cluverse.university.exception.UniversityExceptionMessage;
import cluverse.university.repository.UniversityRepository;
import cluverse.university.service.request.UniversitySearchRequest;
import cluverse.university.service.response.UniversitySummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UniversityReader {

    private final UniversityRepository universityRepository;

    public List<UniversitySummaryResponse> search(UniversitySearchRequest request) {
        List<University> universities = hasKeyword(request)
                ? universityRepository.findActiveUniversitiesByNameContaining(request.keyword().trim())
                : universityRepository.findAllByIsActiveTrueOrderByNameAsc();
        return universities.stream()
                .map(UniversitySummaryResponse::from)
                .toList();
    }

    public University readOrThrow(Long universityId) {
        return universityRepository.findById(universityId)
                .orElseThrow(() -> new NotFoundException(UniversityExceptionMessage.UNIVERSITY_NOT_FOUND.getMessage()));
    }

    private boolean hasKeyword(UniversitySearchRequest request) {
        return request != null && StringUtils.hasText(request.keyword());
    }
}
