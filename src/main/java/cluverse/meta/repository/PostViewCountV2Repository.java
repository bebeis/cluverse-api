package cluverse.meta.repository;

import cluverse.meta.domain.PostViewCountV2;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostViewCountV2Repository extends JpaRepository<PostViewCountV2, Long> {
}
