package cluverse.meta.repository;

import cluverse.meta.domain.PostViewCountOptimistic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostViewCountOptimisticRepository extends JpaRepository<PostViewCountOptimistic, Long> {
}
