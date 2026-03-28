package cluverse.university.service;

import cluverse.university.service.implement.UniversityReader;
import cluverse.university.service.request.UniversitySearchRequest;
import cluverse.university.service.response.UniversityDetailResponse;
import cluverse.university.service.response.UniversitySummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UniversityQueryService {

    private final UniversityReader universityReader;

    public List<UniversitySummaryResponse> searchUniversities(UniversitySearchRequest request) {
        return universityReader.search(request);
    }

    public UniversityDetailResponse getUniversity(Long universityId) {
        return UniversityDetailResponse.from(universityReader.readOrThrow(universityId));
    }
}
