package cluverse.university.service.implement;

import cluverse.university.domain.UniversityCampus;
import cluverse.university.repository.UniversityCampusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UniversityCampusReader {

    private final UniversityCampusRepository universityCampusRepository;

    public List<UniversityCampus> readActiveByUniversityId(Long universityId) {
        return universityCampusRepository.findAllByUniversityIdAndIsActiveTrueOrderByNameAsc(universityId);
    }
}
