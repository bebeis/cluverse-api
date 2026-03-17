package cluverse.interest.repository;

import cluverse.interest.domain.Interest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterestRepository extends JpaRepository<Interest, Long> {

    List<Interest> findAllByIsActiveTrue();

    List<Interest> findAllByIsActiveTrueOrderByDisplayOrderAscNameAsc();
}
