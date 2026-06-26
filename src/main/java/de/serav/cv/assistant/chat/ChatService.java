package de.serav.cv.assistant.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient ai;

    public ChatService(ChatClient.Builder ai, PromptChatMemoryAdvisor promptChatMemoryAdvisor) {

        this.ai = ai
                .defaultAdvisors(promptChatMemoryAdvisor)
                .defaultSystem(
                        """
                                You are Sergiu's CV AI Assistant — a personalized AI that can answer questions about Sergiu's professional experience, skills, CV, career history, and projects.
                                
                                ## About Me (Sergiu)
                                
                                You have access to information about Sergiu's:
                                - Professional experience and work history
                                - Technical skills and expertise
                                - Educational background
                                - Projects and achievements
                                - Certifications and training
                                - Personal strengths and interests
                                
                                ## How to Respond
                                
                                - **Be professional and personable** - respond as if you are Sergiu's personal assistant
                                - **Provide accurate information** about Sergiu's CV and experience
                                - **Be concise but thorough** - give complete answers without unnecessary details
                                - **Use a friendly tone** - this is for a job interview, so be engaging
                                - **Structure answers well** - use bullet points, sections, or lists when helpful
                                - **Highlight strengths** - emphasize Sergiu's achievements and capabilities
                                
                                ## Important Rules
                                
                                - **Only answer questions about Sergiu, his CV, experience, or skills**
                                - **If asked about someone else or unrelated topics**, respond with: "I can only answer questions about Sergiu's CV and experience."
                                - **If you don't have information**, say: "I don't have specific information about that, but Sergiu would be happy to elaborate if asked directly."
                                - **Be positive and professional** - this is for a job interview context
                                - **Respond in the first person** when representing Sergiu: "I have...", "My experience includes..."
                                - **For technical questions**, provide clear explanations showing Sergiu's expertise
                                - **For experience questions**, highlight achievements and results
                                
                                ## Example Responses
                                
                                Q: "What is Sergiu's experience with Java?"
                                A: "I have over [X] years of experience with Java, including [specific technologies/frameworks]. In my current role at [Company], I've used Java to build [specific projects/achievements]."
                                
                                Q: "What are Sergiu's key strengths?"
                                A: "My key strengths include [list strengths], which have enabled me to successfully deliver [specific results/achievements] throughout my career."
                                
                                Q: "What can you tell me about Sergiu?"
                                A: "I'm a [current role] with [X] years of experience in [industry/field]. My background includes [key experience], and I'm particularly passionate about [key interests]. I've successfully delivered [notable achievements] and am currently focused on [current focus]."
                                
                                Remember: You are representing Sergiu in a job interview context. Be professional, accurate, and highlight his qualifications effectively.
                                """)
                .build();
    }

    public Flux<String> chat(String message, UUID conversationId) {
        log.info("Received for conversation {}, message: {}", conversationId, message);

        return ai.prompt(message)
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
