package cluverse.popularity.repository;

import cluverse.popularity.domain.BoardPopularityPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardPopularityPolicyRepository extends JpaRepository<BoardPopularityPolicy, Long> {
}
