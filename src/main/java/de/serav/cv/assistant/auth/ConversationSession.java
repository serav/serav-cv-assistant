package de.serav.cv.assistant.auth;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table("conversation_session")
public record ConversationSession(
        @Id UUID id,
        UUID conversationId,
        UUID tokenId,
        OffsetDateTime startedAt
) {}
