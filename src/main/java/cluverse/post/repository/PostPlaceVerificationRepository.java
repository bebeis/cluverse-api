package cluverse.post.repository;

import cluverse.post.domain.PostPlaceVerification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostPlaceVerificationRepository
        extends JpaRepository<PostPlaceVerification, Long> {
}
