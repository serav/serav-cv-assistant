package de.serav.cv.assistant.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.util.Locale;
import java.util.UUID;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient ai;

    private static final String SYSTEM_PROMPT = """
            You are Sergiu's CV AI Assistant — a personalised AI that answers questions about \
            Sergiu's professional experience, skills, career history, and background.

            ## How to respond

            - Answer **only** questions about Sergiu, his CV, experience, or skills.
            - If asked about someone else or an unrelated topic, say: \
            "I can only answer questions about Sergiu's CV and experience."
            - Speak in the **first person** as Sergiu: "I have…", "My experience includes…"
            - Be concise but complete. Use bullet points or sections when it helps readability.
            - Highlight achievements and strengths; stay professional and friendly.
            - If a question contains a wrong assumption, correct it politely before answering.
            - If specific information is not in the background section below, say: \
            "I don't have that detail, but Sergiu would be happy to answer directly."

            ## Sergiu's professional background

            """;

    public ChatService(ChatClient.Builder ai, PromptChatMemoryAdvisor promptChatMemoryAdvisor,
                       @Value("${CV_CONTENT_ENV}") String cvContent) {

        if (cvContent.isBlank()) {
            throw new IllegalStateException("CV_CONTENT_ENV env var is not set or empty.");
        }
        log.info("CV content loaded ({} chars)", cvContent.length());

        this.ai = ai
                .defaultAdvisors(promptChatMemoryAdvisor)
                .defaultSystem(SYSTEM_PROMPT + cvContent + "\n\n{langInstruction}")
                .build();
    }

    public Flux<String> chat(String message, UUID conversationId, Locale locale) {
        log.info("Received for conversation {}, message: {}", conversationId, message);

        var langInstruction = Locale.GERMAN.getLanguage().equals(locale.getLanguage())
                ? "Always respond in German (Deutsch)."
                : "Always respond in English.";

        return ai.prompt()
                .user(message)
                .system(s -> s.param("langInstruction", langInstruction))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content()
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("AI service HTTP error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
                    return switch (ex.getStatusCode().value()) {
                        case 401, 403 -> Flux.just("Warning: Authentication error: The AI service rejected the request. Please check the API key configuration.");
                        case 429 -> Flux.just("Warning: Rate limit reached: Too many requests. Please wait a moment and try again.");
                        case 503, 502 -> Flux.just("Warning: AI service unavailable: The service is temporarily down. Please try again later.");
                        default -> Flux.just("Warning: AI service error (" + ex.getStatusCode().value() + "): " + ex.getStatusText() + ". Please try again.");
                    };
                })
                .onErrorResume(java.net.ConnectException.class, ex -> {
                    log.error("Network connection error to AI service", ex);
                    return Flux.just("Warning: Network error: Could not reach the AI service. Please check the network configuration or contact your platform administrator.");
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("Unexpected error calling AI service", ex);
                    return Flux.just("Warning: Unexpected error: Something went wrong. Please try again or contact support.");
                });
    }
}
