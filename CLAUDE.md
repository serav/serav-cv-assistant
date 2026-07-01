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

### Routes

- **`ChatView`** → route **`/`** — the only chat view. Token-gated via Spring Security; streams AI responses token-by-token using Vaadin Push.
- **`LoginView`** → route **`/login`** — token entry form, publicly accessible.

### Configuration & profiles

- `application.yml` — the single committed config file. All environment-specific values are injected via `${ENV_VAR}` placeholders. Production defaults (`vaadin.productionMode: true`).
- `application-local.yml` — gitignored local overrides (local DB, Together.ai key, Vaadin dev mode). Activate with `-Dspring.profiles.active=local`.
- Switching LLM provider means swapping the Spring AI starter in `pom.xml` (`spring-ai-starter-model-ollama` ↔ `spring-ai-starter-model-openai`) plus the matching config block in `application.yml`.

### Skills (CV knowledge base)

CV content lives in SKILL.md files (gitignored) packed into a password-protected `src/main/resources/skills/skills.zip`. On startup, `ChatService` extracts the ZIP to a temp directory using the password from `${SKILLS_ZIP_UNPACK}` and loads the skills into a `SkillsTool` (single tool named `Skill`). The LLM calls this tool with a skill name (`command` parameter) to retrieve CV content before answering.

### Deployment (Clever Cloud)

Deployed as a Spring Boot fat JAR on Clever Cloud. All credentials and config come from Clever Cloud environment variables. DB is a Clever Cloud PostgreSQL add-on; credentials injected via `POSTGRESQL_ADDON_*` env vars.

## Rules

**Environment variables are set only in `application.yml` — Java code reads Spring properties, never env vars directly.**

Map every env var to a Spring property key in `application.yml` using `${ENV_VAR}` syntax, then inject with `@Value("${spring.property.key}")` or `@ConfigurationProperties`. Never use `System.getenv()` or `System.getProperty()` in application code.

## Key versions

Spring Boot 4.0.6, Spring AI 2.0.0-M4, Vaadin 25.1.5, Java 25, React 19. `src/main/frontend/generated/` is Vaadin-generated — do not edit by hand.
