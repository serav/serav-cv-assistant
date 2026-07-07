package de.serav.cv.assistant.console;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConversationSummary(
        UUID conversationId,
        String label,
        OffsetDateTime startedAt,
        long userMessages,
        long totalMessages
) {}
