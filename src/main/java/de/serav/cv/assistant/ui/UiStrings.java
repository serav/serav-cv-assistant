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
                    "What is Sergiu's current role?",
                    "What are his key technical skills?",
                    "Tell me about his education",
                    "What languages does he speak?",
                    "Describe his leadership experience",
                    "What are his hobbies and interests?"
            ),
            "Quick questions",
            "Ask anything about Sergiu…",
            "Sign out",
            "Greet the visitor in 2–3 friendly sentences. You are Sergiu's CV assistant. Invite them to ask anything about his experience, skills, or background. Respond in English.",
            "Message too long (max %d characters)."
    );

    static final UiStrings DE = new UiStrings(
            List.of(
                    "Was ist Sergius aktuelle Position?",
                    "Was sind seine wichtigsten technischen Fähigkeiten?",
                    "Erzähl mir von seiner Ausbildung",
                    "Welche Sprachen spricht er?",
                    "Beschreibe seine Führungserfahrung",
                    "Was sind seine Hobbys und Interessen?"
            ),
            "Schnellfragen",
            "Frag mich alles über Sergiu…",
            "Abmelden",
            "Begrüße den Besucher in 2–3 freundlichen Sätzen auf Deutsch. Du bist Sergius Lebenslauf-Assistent. Lade ihn ein, alles über seine Erfahrung, Fähigkeiten oder seinen Hintergrund zu fragen. Antworte auf Deutsch.",
            "Nachricht zu lang (max. %d Zeichen)."
    );
}
