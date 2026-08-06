package cluverse.university.repository;

import cluverse.university.domain.UniversityCampus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UniversityCampusRepository extends JpaRepository<UniversityCampus, Long> {

    List<UniversityCampus> findAllByUniversityIdAndIsActiveTrueOrderByNameAsc(Long universityId);
}
