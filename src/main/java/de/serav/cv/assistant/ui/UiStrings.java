package de.serav.cv.assistant.ui;

import java.util.List;
import java.util.Locale;

record UiStrings(
        List<String> quickQuestions,
        String quickQuestionsLabel,
        String inputPlaceholder,
        String signOut,
        String greetingPrompt,
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
            "Greet the visitor in 2–3 friendly sentences. You are Sergiu's CV assistant. Invite them to ask anything about his experience, skills, or background. Respond in English.",
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
            "Begrüße den Besucher in 2–3 freundlichen Sätzen auf Deutsch. Du bist Sergius Lebenslauf-Assistent. Lade ihn ein, alles über seine Erfahrung, Fähigkeiten oder seinen Hintergrund zu fragen. Antworte auf Deutsch.",
            "Nachricht zu lang (max. %d Zeichen)."
    );
}
