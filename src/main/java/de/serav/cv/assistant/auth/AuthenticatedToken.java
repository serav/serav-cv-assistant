package de.serav.cv.assistant.auth;

import java.util.UUID;

public record AuthenticatedToken(UUID tokenId, String label) {}
