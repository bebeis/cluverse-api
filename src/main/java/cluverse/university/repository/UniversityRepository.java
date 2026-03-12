package cluverse.university.repository;

import cluverse.university.domain.University;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniversityRepository extends JpaRepository<University, Long> {

    boolean existsById(Long id);
}
