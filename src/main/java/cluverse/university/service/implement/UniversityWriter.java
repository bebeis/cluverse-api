package cluverse.university.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.university.domain.University;
import cluverse.university.exception.UniversityExceptionMessage;
import cluverse.university.repository.UniversityRepository;
import cluverse.university.service.request.UniversityCreateRequest;
import cluverse.university.service.request.UniversityUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class UniversityWriter {

    private final UniversityRepository universityRepository;

    public University create(UniversityCreateRequest request) {
        validateNameNotExists(request.name());
        University university = University.create(
                request.name(),
                request.emailDomain(),
                request.badgeImageUrl(),
                request.address(),
                request.isActive()
        );
        return universityRepository.save(university);
    }

    public void update(University university, UniversityUpdateRequest request) {
        validateNameNotExists(request.name(), university.getId());
        university.update(
                request.name(),
                request.emailDomain(),
                request.badgeImageUrl(),
                request.address(),
                request.isActive()
        );
    }

    private void validateNameNotExists(String universityName) {
        if (universityRepository.existsByName(universityName.trim())) {
            throw new BadRequestException(UniversityExceptionMessage.UNIVERSITY_NAME_ALREADY_EXISTS.getMessage());
        }
    }

    private void validateNameNotExists(String universityName, Long universityId) {
        if (universityRepository.existsByNameAndIdNot(universityName.trim(), universityId)) {
            throw new BadRequestException(UniversityExceptionMessage.UNIVERSITY_NAME_ALREADY_EXISTS.getMessage());
        }
    }
}
