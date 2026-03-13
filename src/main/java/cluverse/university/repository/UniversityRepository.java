package cluverse.university.repository;

import cluverse.university.domain.University;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UniversityRepository extends JpaRepository<University, Long> {

    boolean existsById(Long id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<University> findAllByIsActiveTrueOrderByNameAsc();

    @Query("""
            select u
            from University u
            where u.isActive = true
              and lower(u.name) like lower(concat('%', :keyword, '%'))
            order by u.name asc
            """)
    List<University> findActiveUniversitiesByNameContaining(String keyword);
}
