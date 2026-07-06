package de.serav.cv.assistant.ui;

import java.util.List;
import java.util.Locale;

record UiStrings(
        List<String> quickQuestions,
        String quickQuestionsLabel,
        String inputPlaceholder,
        String signOut,
        String greetingMessage,
        String messageTooLong
) {
    static UiStrings forLocale(Locale locale) {
        return locale.getLanguage().equals("de") ? DE : EN;
    }

    static final UiStrings EN = new UiStrings(
            List.of(
                    "What is your current role?",
                    "What are your key technical skills?",
                    "Tell me about your education",
                    "What languages do you speak?",
                    "What do former employers say about you?",
                    "What is your experience with cloud and AI?"
            ),
            "Quick questions",
            "Ask anything about Sergiu…",
            "Sign out",
            "Hi! I'm Sergiu's CV assistant. Ask me anything about my professional experience, technical skills, education, or background — use the quick questions on the left or type your own.",
            "Message too long (max %d characters)."
    );

    static final UiStrings DE = new UiStrings(
            List.of(
                    "Was ist Ihre aktuelle Position?",
                    "Was sind Ihre wichtigsten technischen Fähigkeiten?",
                    "Erzählen Sie mir von Ihrer Ausbildung",
                    "Welche Sprachen sprechen Sie?",
                    "Was sagen frühere Arbeitgeber über Sie?",
                    "Was sind Ihre Erfahrungen mit Cloud und KI?"
            ),
            "Schnellfragen",
            "Frag mich alles über Sergiu…",
            "Abmelden",
            "Hallo! Ich bin Sergius Lebenslauf-Assistent. Frag mich alles über meine Berufserfahrung, technischen Fähigkeiten, Ausbildung oder meinen Werdegang — nutze die Schnellfragen auf der linken Seite oder stell deine eigene Frage.",
            "Nachricht zu lang (max. %d Zeichen)."
    );
}
