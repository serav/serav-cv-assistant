# Sergiu Avram's CV AI Assistant

An open-source, interactive CV assistant that lets recruiters and interviewers have a natural conversation about my professional background. Ask it anything — current role, tech stack, education, hobbies — and it answers in first-person, as if I were there.

Live on **[Clever Cloud](https://www.clever-cloud.com)**, powered by LLMs from **[Together AI](https://www.together.ai)**.

---

## What it does

- Visitors enter an access token and start chatting with an AI that knows my CV
- Conversations are streamed token-by-token in real time
- Chat history is persisted per session in PostgreSQL, so context carries across turns
- Automatically switches between **English and German** based on the browser's language setting, with a manual toggle in the header
- Each access token is limited in uses and has an expiry date — useful for sharing with specific people

---

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 25 (virtual threads enabled) |
| Framework | Spring Boot 4.0.6 |
| AI | Spring AI 2.0.0-M4 |
| LLM provider | Together AI (OpenAI-compatible endpoint) |
| LLM model | Qwen/Qwen3-Max |
| UI | Vaadin Flow 25.1.5 (React 19 under the hood) |
| Security | Spring Security — token-based auth |
| Database | PostgreSQL (chat memory + token store) |
| Deployment | Clever Cloud (Java runtime + PostgreSQL add-on) |

---

## Architecture

```
Browser (Vaadin UI)
       │
       ▼
  ChatView  ──────────►  ChatService  ──────────►  Together AI
       │                        │                    (Qwen3-Max)
       │                        │ streams Flux<String>
       │                   ChatClient
       │                  (Spring AI)
       │
  LoginView  ──────────►  TokenAuthenticationProvider
                                │
                         PostgreSQL (cv_assistant schema)
                         ├── access_token
                         ├── conversation_session
                         └── spring_ai_chat_memory
```

**Request flow:** Vaadin view → `ChatService` → Spring AI `ChatClient` → Together AI, streamed back token-by-token via reactive `Flux<String>` and Vaadin server push (`@Push`).

**Chat memory:** Spring AI's `PromptChatMemoryAdvisor` with a JDBC-backed `MessageWindowChatMemory` (last 20 messages). Keyed by a per-session UUID stored in `VaadinSession`.

---

## Security

Authentication uses short-lived access tokens with a format inspired by GitHub Personal Access Tokens:

```
scv_<32 random base62 characters>
```

- Tokens are stored as **SHA-256 hashes** in the database (same approach as GitHub PATs — high-entropy tokens make brute force infeasible regardless of hash speed)
- Each token has a label, expiry date, and a maximum usage count
- Failed authentication does not leak whether a token exists
- Session fixation protection via `changeSessionId()` after login
- All application tables are isolated under a dedicated `cv_assistant` PostgreSQL schema

To generate a new token, use `TokenGenerator.generate()` from the `auth` package — it returns the plaintext token once (store it securely) and you insert the SHA-256 hash into the `access_token` table.

---

## License

MIT
