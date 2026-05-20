package de.serav.flowmetrix.ai.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient ai;

    public ChatService(ChatClient.Builder ai, PromptChatMemoryAdvisor promptChatMemoryAdvisor) {

        var skillTool = SkillsTool
                .builder()
                .addSkillsResource(new ClassPathResource("/skills"))
                .build();

        this.ai = ai
                .defaultToolCallbacks(skillTool)
                .defaultAdvisors(promptChatMemoryAdvisor)
                .defaultSystem(
                        """
                                You are a helpful assistant for FlowMetrix — a Python tool that collects GitHub Actions workflow run data and generates an interactive HTML report published to GitHub Pages.
                                
                                ## Core concepts
                                
                                ### Time metrics
                                FlowMetrix calculates three time values per workflow run (all from the GitHub REST API):
                                - **Total Duration**: wall-clock time (`updated_at − run_started_at`)
                                - **Idle Time**: time when no job was running (runner queue wait, gaps between jobs). Calculated by merging overlapping job intervals and subtracting from total duration.
                                - **Execution Time**: is the workflow’s real execution time. It is calculated as: `exec_dur = total_dur - wait_dur`                                
                                
                                ### Operation modes
                                - **`mode: snapshot`**: Two-step process. `snapshot_repositories.py` discovers repos via wildcard patterns and saves them to `cache/snapshot.yml`. Then `create_workflow_metrics.py` reads from that snapshot. Best for many repos.
                                - **`mode: repositories`**: Single step. Repos and workflows are listed explicitly in `metrics_config.yml`. No discovery step needed. Best for a few known repos.
                                
                                ### Report features
                                - Multi-repository overview table (sortable, searchable)
                                - Trend chart with linear trend line (green = improving, red = worsening)
                                - Per-job execution times and step-level breakdown
                                - Exec time vs idle time separation
                                - Color-coded job durations: 🟢 at/below average · 🟠 up to 25% above · 🔴 >25% above
                                
                                ## Skills
                                For detailed questions, load the appropriate skill:
                                - **setup** — forking, PAT token, GitHub Pages, first run
                                - **configuration** — full `metrics_config.yml` reference with examples
                                - **caching** — how snapshot and workflow cache work
                                - **local-dev** — running the tool locally
                                - **troubleshooting** — empty report, rate limits, common errors
                                - **report-reading** — how to read and interpret the FlowMetrix HTML report
                                - **gh-workflow** — how the GitHub Actions workflow works in FlowMetrix
                                - **execution-time** - how FlowMetrix calculates total, wait, and exec duration
                                
                                ## Behavior rules
                                - **You only answer questions about FlowMetrix.** If a question is not about FlowMetrix, respond immediately with: "I can only answer questions about FlowMetrix." Do not try to relate the question back to FlowMetrix. Do not ask for clarification. Do not partially answer.
                                - **Do not answer questions about alternatives, competing tools, or comparisons.** Respond with: "I can only answer questions about FlowMetrix. For alternative tools, please search on your own."
                                - Be concise and practical — users are developers who want quick answers.
                                - When relevant, point to the exact config field or step that applies.
                                - If a question is ambiguous (e.g. "why is my report empty?"), ask one clarifying question before answering.
                                - For deep-dive topics, use the matching skill file rather than answering from memory.
                                """)
                .build();
    }

    public Flux<String> chat(String message, UUID conversationId) {
        return ai.prompt(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content()
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("AI service HTTP error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
                    return switch (ex.getStatusCode().value()) {
                        case 401, 403 -> Flux.just("⚠️ **Authentication error:** The AI service rejected the request. Please check the API key configuration.");
                        case 429 -> Flux.just("⚠️ **Rate limit reached:** Too many requests. Please wait a moment and try again.");
                        case 503, 502 -> Flux.just("⚠️ **AI service unavailable:** The service is temporarily down. Please try again later.");
                        default -> Flux.just("⚠️ **AI service error (" + ex.getStatusCode().value() + "):** " + ex.getStatusText() + ". Please try again.");
                    };
                })
                .onErrorResume(java.net.ConnectException.class, ex -> {
                    log.error("Network connection error to AI service", ex);
                    return Flux.just("⚠️ **Network error:** Could not reach the AI service. Please check the network configuration or contact your platform administrator.");
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("Unexpected error calling AI service", ex);
                    return Flux.just("⚠️ **Unexpected error:** Something went wrong. Please try again or contact support.");
                });
    }
}
