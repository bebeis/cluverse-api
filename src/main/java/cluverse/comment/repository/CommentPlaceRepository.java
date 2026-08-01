package cluverse.comment.repository;

import cluverse.comment.domain.CommentPlace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentPlaceRepository extends JpaRepository<CommentPlace, Long> {
}
