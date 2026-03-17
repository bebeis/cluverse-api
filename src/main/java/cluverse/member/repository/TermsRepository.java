package cluverse.member.repository;

import cluverse.member.domain.Terms;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TermsRepository extends JpaRepository<Terms, Long> {

    List<Terms> findAllByIsActiveTrueAndIsRequiredTrue();

    List<Terms> findAllByIsActiveTrueOrderByIsRequiredDescIdAsc();
}
