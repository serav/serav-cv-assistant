package de.serav.cv.assistant.console;

import java.time.LocalDateTime;

public record ChatMessage(String type, String content, LocalDateTime timestamp) {}
