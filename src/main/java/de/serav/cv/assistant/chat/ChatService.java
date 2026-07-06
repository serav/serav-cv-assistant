package de.serav.cv.assistant.chat;

import net.lingala.zip4j.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient ai;

    private static final String SYSTEM_PROMPT = """
            You are Sergiu's CV AI Assistant — a personalised AI that answers questions about \
            Sergiu's professional experience, skills, career history, and background.

            You have access to a tool called getCvSection that retrieves accurate information \
            about Sergiu. Always call getCvSection with the appropriate section name before answering.

            ## How to respond

            - Always call getCvSection first to retrieve relevant information before composing your answer.
            - Answer **only** questions about Sergiu, his CV, experience, skills, qualities, or reputation. \
            This includes opinion and character questions such as "Is he a good developer?", \
            "What are his strengths?", or "Would you recommend him?" — answer these confidently \
            using the professional-character and work-experience sections.
            - If asked about anything genuinely unrelated to Sergiu — such as general coding tasks, \
            building applications, algorithms, trivia, or other people — respond only with: \
            "I'm Sergiu's CV assistant. I can only answer questions about his professional experience and background."
            - Speak in the **first person** as Sergiu: "I have…", "My experience includes…"
            - Be concise but complete. Use bullet points or sections when it helps readability.
            - Highlight achievements and strengths; stay professional and friendly.
            - If a question contains a wrong assumption, correct it politely before answering.
            - If the relevant skill contains no mention of the topic, answer in first person \
            without referencing the CV as a document. For example: \
            "C++? That's not something I've worked with professionally."
            - Always respond in the same language the user writes in.
            - **Never write code, scripts, algorithms, application designs, or any technical output.** \
            Requests like "write a function", "build an app", "design a system", or "show me how to implement X" \
            are off-topic and must be refused with the standard off-topic response above.
            """;

    public ChatService(ChatClient.Builder ai, PromptChatMemoryAdvisor promptChatMemoryAdvisor,
                       ResourceLoader resourceLoader,
                       @Value("${app.skills-unpack}") String skillsZipCred) throws IOException {

        var skillsTool = loadSkills(resourceLoader, skillsZipCred);

        this.ai = ai
                .defaultAdvisors(promptChatMemoryAdvisor)
                .defaultToolCallbacks(skillsTool)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    private record CvSectionQuery(
            @ToolParam(description = "The CV section to retrieve. One of: personal-summary, work-experience, technical-skills, education-and-training, professional-character, languages-and-methods")
            String section) {}

    private static org.springframework.ai.tool.ToolCallback loadSkills(
            ResourceLoader resourceLoader, String password) throws IOException {

        var zipResource = resourceLoader.getResource("classpath:skills/skills.zip");

        var tempZip = Files.createTempFile("cv-skills", ".zip");
        Files.copy(zipResource.getInputStream(), tempZip, StandardCopyOption.REPLACE_EXISTING);

        var tempDir = Files.createTempDirectory("cv-skills-extracted");
        try (var zipFile = new ZipFile(tempZip.toFile(), password.toCharArray())) {
            zipFile.extractAll(tempDir.toString());
        }
        Files.delete(tempZip);

        Map<String, String> sections = new HashMap<>();
        try (var dirs = Files.list(tempDir)) {
            dirs.filter(Files::isDirectory).forEach(dir -> {
                var skillFile = dir.resolve("SKILL.md");
                if (Files.exists(skillFile)) {
                    try {
                        sections.put(dir.getFileName().toString(), Files.readString(skillFile));
                    } catch (IOException e) {
                        log.warn("Could not read skill file: {}", skillFile);
                    }
                }
            });
        }
        log.info("Loaded {} CV sections: {}", sections.size(), sections.keySet());

        return FunctionToolCallback
                .builder("getCvSection", (CvSectionQuery q) ->
                        sections.getOrDefault(q.section(), "No information found for section: " + q.section()))
                .description("Retrieves a section of Sergiu's CV. Call this before answering any question about Sergiu.")
                .inputType(CvSectionQuery.class)
                .build();
    }

    public Flux<String> chat(String message, UUID conversationId) {
        log.info("Received for conversation {}, message: {}", conversationId, message);

        return ai.prompt()
                .user(message)
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
