package cluverse.recruitment.repository;

import cluverse.recruitment.domain.ApplicationChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationChatMessageRepository extends JpaRepository<ApplicationChatMessage, Long> {
}
