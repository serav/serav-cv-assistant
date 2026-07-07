package de.serav.cv.assistant.console;

public record LabelStats(String label, long withMessages, long withoutMessages) {
    public long total() { return withMessages + withoutMessages; }
}
