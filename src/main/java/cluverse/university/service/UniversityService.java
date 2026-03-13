package cluverse.university.service;

import cluverse.university.service.request.UniversityCreateRequest;
import cluverse.university.service.request.UniversitySearchRequest;
import cluverse.university.service.request.UniversityUpdateRequest;
import cluverse.university.service.response.UniversityDetailResponse;
import cluverse.university.service.response.UniversitySummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UniversityService {

    public List<UniversitySummaryResponse> searchUniversities(UniversitySearchRequest request) {
        throw unsupported();
    }

    public UniversityDetailResponse getUniversity(Long universityId) {
        throw unsupported();
    }

    public UniversityDetailResponse createUniversity(Long memberId, UniversityCreateRequest request) {
        throw unsupported();
    }

    public UniversityDetailResponse updateUniversity(Long memberId, Long universityId, UniversityUpdateRequest request) {
        throw unsupported();
    }

    private UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("학교 서비스는 아직 구현되지 않았습니다.");
    }
}
