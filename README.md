# Spring AI Agentic Application

An agentic Spring Boot app (Spring AI) that reasons with a **local Ollama** model and
autonomously invokes **local tools** and **remote MCP tools**, with **pgvector** for RAG.

## Stack
- Spring Boot 3.5 / Spring AI 1.1.8 (Java 21, Gradle)
- MCP SDK 0.18.3 (SSE + Streamable-HTTP transports)
- Ollama chat model: `qwen2.5:7b` (tool-calling capable)
- Ollama embeddings: `nomic-embed-text` (768-dim)
- Two MCP servers, each with its own auth:
  - `local` — SSE at `http://localhost:8090/sse`, HTTP Basic auth
  - `parallel-search` — Streamable-HTTP at `https://search.parallel.ai/mcp`, `x-api-key` token
- pgvector (Docker, host port **5422**)

## Prerequisites
```bash
# Java 21 (not currently installed on this machine)
brew install openjdk@21

# Ollama models
ollama pull qwen2.5:7b
ollama pull nomic-embed-text
```
pgvector and your MCP server are already running.

## Configure
Edit [src/main/resources/application.yml](src/main/resources/application.yml):
- `agent.mcp.local.*` -> your local MCP server URL + Basic auth credentials
- `agent.mcp.parallel-search.*` -> Parallel.ai URL + API key (or set `PARALLEL_API_KEY` env var)
- `spring.datasource.*` -> pgvector credentials (defaults: postgres/postgres @ localhost:5422)
- `spring.ai.ollama.chat.options.model` -> your chat model

## Run
```bash
./gradlew bootRun
```
App starts on http://localhost:8081

## Endpoints
```bash
# List all tools the agent can call (local + MCP)
curl http://localhost:8081/api/agent/tools

# Chat with the agent (it decides which tools to invoke)
curl -X POST http://localhost:8081/api/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"What time is it, and what MCP tools do you have?","conversationId":"c1"}'

# RAG: ingest text into pgvector
curl -X POST http://localhost:8081/api/rag/ingest \
  -H 'Content-Type: application/json' \
  -d '{"text":"Spring AI unifies AI model access for Java apps."}'

# RAG: ask grounded in ingested docs
curl -X POST http://localhost:8081/api/rag/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"What does Spring AI do?"}'
```

## How tools are wired
- **Local tools**: methods annotated with `@Tool` in
  [AgentTools.java](src/main/java/com/example/agent/AgentTools.java).
- **MCP tools**: both MCP servers are built manually in
  [McpConfig.java](src/main/java/com/example/agent/McpConfig.java) — each gets its own
  `WebClient` with the right auth header (Basic auth vs `x-api-key`). Their tools are
  aggregated into a single `ToolCallbackProvider` and attached to the `ChatClient` in
  [AgentConfig.java](src/main/java/com/example/agent/AgentConfig.java).
