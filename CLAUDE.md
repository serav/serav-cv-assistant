# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A personal "CV AI assistant" web app: a chat UI where a visitor (e.g. an interviewer) can ask questions about Sam/Sergiu Avram's professional experience. Spring Boot + Spring AI backend, Vaadin Flow (React) UI, PostgreSQL for chat memory, Ollama as the local LLM.

## Commands

No Maven wrapper — use a system `mvn` (requires JDK 25).

Maven must be invoked with JDK 25 explicitly (system `mvn` defaults to whichever JAVA_HOME the shell has):

```bash
# Start the database (Postgres on :5432, db "cv", myuser/mypass)
docker compose up -d        # or: podman compose up -d

# Run locally (default profile -> Ollama at localhost:11434, model qwen2.5:7b)
JAVA_HOME=/Users/sam/Library/Java/JavaVirtualMachines/azul-25.0.3/Contents/Home mvn spring-boot:run

# Build production jar (compiles + bundles Vaadin frontend via Vite)
JAVA_HOME=<jdk25-path> mvn clean package -Pproduction

# Run tests
JAVA_HOME=<jdk25-path> mvn test
JAVA_HOME=<jdk25-path> mvn test -Dtest=AgentApplicationTests#contextLoads   # single test
```

`mvn test` runs `AgentApplicationTests.contextLoads`, a `@SpringBootTest` that boots the full context — it needs a reachable Postgres (chat-memory schema init) and Ollama, so start `docker compose` first.

Prerequisites for local dev: Ollama running with the `qwen2.5:7b` and `nomic-embed-text` models pulled.

## Architecture

Request flow: Vaadin view → `ChatService` → Spring AI `ChatClient` → Ollama, streamed back token-by-token.

- **`ChatService`** (`chat/ChatService.java`) — the core. Wraps `ChatClient` with a hardcoded `defaultSystem` prompt (the assistant's persona and guardrails: only answers about Sam, first-person voice, interview tone). `chat(message, conversationId)` returns a reactive `Flux<String>` of streamed content with per-status-code error fallbacks. Conversation memory is keyed by a `UUID` passed as `ChatMemory.CONVERSATION_ID`.
- **`ChatClientConfig`** (`chat/ChatClientConfig.java`) — wires a `PromptChatMemoryAdvisor` over a JDBC-backed `MessageWindowChatMemory` (last 20 messages, Postgres). The advisor is applied as a default advisor in `ChatService`.
- **`SeravCVAIChat`** — Spring Boot entrypoint; `@Push` enables Vaadin server push (needed for streaming tokens into the UI).

### Three chat views (three routes, very different)

- **`CvChatView`** → route **`/`** (default) — the primary view. Two-column layout (profile sidebar + chat). Wires `ChatService` for real streaming AI. **This is the main view to develop.**
- **`ChatView`** → route **`/mis`** — an older, simpler AI-backed view using viritin `MarkdownMessage` bubbles. Still wires `ChatService`.
- **`CvChatViewNew`** → route **`/per`** — a standalone view with a **hardcoded CV knowledge base** (constants like `CURRENT_JOB`, `EDUCATION`) answered by a local `generateAnswer(...)` string matcher. It does **not** call `ChatService` or any LLM.

### Configuration & profiles

- `application.yml` — **default/local** profile: Ollama base-url, qwen2.5:7b chat model, nomic-embed-text embeddings, local Postgres, virtual threads on, Vaadin dev mode (`productionMode: false`, hotdeploy). Commented-out OpenAI/Together blocks show the alternative provider wiring.
- `application-cloud.properties` — **cloud** profile (activated by `SPRING_PROFILES_ACTIVE=cloud`), currently fully commented out; reads DB/creds from Cloud Foundry `vcap.services` bindings.
- Switching LLM provider means swapping the Spring AI starter in `pom.xml` (`spring-ai-starter-model-ollama` ↔ `spring-ai-starter-model-openai`, both present, OpenAI commented) plus the matching config block.

### Deployment (Cloud Foundry)

`manifest.yml` + helper scripts deploy to CF. Scripts contain placeholders (`<url>`, `<org>`, `<space>`, `<service_url>`) — fill before use.
- `cf_login_and_push.sh` — `mvn clean package -Pproduction -DskipTests` then `cf login --sso` then `cf push`.
- `cf_login_db_tunel_dev.sh` — opens an SSH tunnel (`cf ssh -L`) to the bound Postgres service for local debugging.

## Naming caveat (rename in progress)

The project was renamed **FlowMetrix AI Chat → Serav CV AI Chat**. Java packages are now `de.serav.cv.assistant`, but legacy `flowmetrix` names persist in config and must not be assumed consistent: `spring.application.name=flowmetrix-ai-chat`, the CF app/service names in `manifest.yml`, and the route `flowmetrix-chat.com`. When touching config, check which name a given resource actually uses rather than assuming.

The `src/main/resources/skills/*/SKILL.md` files are leftover **FlowMetrix** docs (GitHub Actions metrics tooling), unrelated to this CV app. They were meant to feed a Spring AI `SkillsTool`, but that wiring is **commented out** in `ChatService` — they are currently dead/inert. Don't treat them as docs for this project.

## Key versions

Spring Boot 4.0.6, Spring AI 2.0.0-M4, Vaadin 25.1.5, Java 25, React 19. `src/main/frontend/generated/` is Vaadin-generated — do not edit by hand.
