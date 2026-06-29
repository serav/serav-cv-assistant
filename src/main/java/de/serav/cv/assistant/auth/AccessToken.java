package de.serav.cv.assistant.auth;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table("access_token")
public record AccessToken(
        @Id UUID id,
        String token,
        String label,
        OffsetDateTime validUntil,
        int maxAttempts,
        int attemptsUsed
) {
    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(validUntil);
    }

    public boolean isExhausted() {
        return attemptsUsed >= maxAttempts;
    }

    public AccessToken incrementUsage() {
        return new AccessToken(id, token, label, validUntil, maxAttempts, attemptsUsed + 1);
    }
}
