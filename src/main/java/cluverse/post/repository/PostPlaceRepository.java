package cluverse.post.repository;

import cluverse.post.domain.PostPlace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostPlaceRepository extends JpaRepository<PostPlace, Long> {
}
