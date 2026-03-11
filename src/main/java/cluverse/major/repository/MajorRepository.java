package cluverse.major.repository;

import cluverse.major.domain.Major;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MajorRepository extends JpaRepository<Major, Long> {

    List<Major> findAllByParentIdIsNull();
    List<Major> findAllByParentId(Long parentId);
}
