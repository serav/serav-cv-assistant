package de.serav.cv.assistant.auth;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface ConversationSessionRepository extends CrudRepository<ConversationSession, UUID> {

    List<ConversationSession> findByTokenId(UUID tokenId);

    @Modifying
    @Query("INSERT INTO conversation_session (conversation_id, token_id) " +
           "VALUES (:conversationId, :tokenId) ON CONFLICT (conversation_id) DO NOTHING")
    void registerIfAbsent(UUID conversationId, UUID tokenId);
}
