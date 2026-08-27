# API Reference

## Endpoint Summary

| Method | Path | Status | Purpose | Request Body | Response | Source |
| --- | --- | --- | --- | --- | --- | --- |
| `POST` | `/api/agent/chat` | Active | Send a user message to the agent and receive a model-generated response that may use local and MCP tools. | `ChatRequest { message, conversationId }` | `{ conversationId, response }` | `src/main/java/com/pkm/agent/controller/AgentController.java` |
| `GET` | `/api/agent/tools` | Active | List currently exposed tool definitions from the aggregated MCP tool provider. | None | `[{ name, description }]` | `src/main/java/com/pkm/agent/controller/AgentController.java` |
| `POST` | `/api/agent/document` | Active | Trigger the documentation agent workflow that scans and rewrites `.java` files through `DocumentationTools`. | None | `{ summary }` | `src/main/java/com/pkm/agent/controller/DocumentationController.java` |
| `POST` | `/api/rag/ingest` | Active | Embed and store a piece of text in the configured pgvector-backed vector store. | `IngestRequest { text }` | `{ status, id }` | `src/main/java/com/pkm/agent/controller/RagController.java` |
| `POST` | `/api/rag/ask` | Inactive (commented out) | Planned grounded question answering over ingested documents. | `AskRequest { question }` | `{ response }` | `src/main/java/com/pkm/agent/controller/RagController.java` |

## Shared Runtime Context

- Base server port: `8081` (`src/main/resources/application.yml`)
- Main application: Spring Boot + Spring AI (`build.gradle`, `AgentApplication.java`)
- Chat orchestration: `AgentConfig.agentChatClient(...)`
- MCP client wiring: `McpConfig`
- Local tools: `AgentTools`, `DocumentationTools`
- Cross-cutting prompt/response logging: `ChatModelLoggingAspect`

## `POST /api/agent/chat`

**Purpose**  
Accepts a user message, resolves a conversation ID, and sends the request through the configured `ChatClient`.

**Implementation**  
`src/main/java/com/pkm/agent/controller/AgentController.java`

**Request body**

```json
{
  "message": "string",
  "conversationId": "string"
}
```

**Observed behavior**

- `conversationId` is optional in practice.
- If `conversationId` is `null` or blank, the controller substitutes `"default"`.
- The controller passes the resolved ID through `ChatMemory.CONVERSATION_ID`.
- The `ChatClient` is configured with:
  - a system prompt
  - local tools from `AgentTools`
  - local tools from `DocumentationTools`
  - MCP tool callbacks from `mcpToolProvider`
  - conversation memory and logging advisors

**Response**

```json
{
  "conversationId": "string",
  "response": "string"
}
```

**Notes**

- The model may invoke local tools and/or any reachable MCP tools before returning the final answer.
- Prompt/response logging is applied around `ChatModel.call(..)` by `ChatModelLoggingAspect`.

## `GET /api/agent/tools`

**Purpose**  
Returns tool definitions visible through the aggregated `ToolCallbackProvider`.

**Implementation**  
`src/main/java/com/pkm/agent/controller/AgentController.java`

**Request body**  
None.

**Response**

```json
[
  {
    "name": "string",
    "description": "string"
  }
]
```

**Observed behavior**

- The implementation iterates only over `mcpToolProvider.getToolCallbacks()`.
- As written, this endpoint reports reachable MCP tool definitions, not the local `@Tool` methods from `AgentTools` or `DocumentationTools`.
- If an MCP server is unavailable during startup, `McpConfig.safeInitialize(...)` excludes it, so its tools will not appear here.

## `POST /api/agent/document`

**Purpose**  
Triggers a fixed instruction prompt that tells the model to use `DocumentationTools` to scan Java files, read them, add Javadoc, and write them back.

**Implementation**  
`src/main/java/com/pkm/agent/controller/DocumentationController.java`

**Request body**  
None.

**Response**

```json
{
  "summary": "string"
}
```

**Observed behavior**

- The endpoint always uses the controller-defined `DOC_INSTRUCTION`.
- `DocumentationTools` restricts access to `.java` files under `agent.doc.root-path`.
- The workflow can modify repository files as a side effect when the tool chooses to write updated content.

**Related tool methods**

- `scanForMissingJavadoc()`
- `readJavaFile(relativePath)`
- `writeJavaFile(relativePath, content)`

## `POST /api/rag/ingest`

**Purpose**  
Stores input text in the configured vector store.

**Implementation**  
`src/main/java/com/pkm/agent/controller/RagController.java`

**Request body**

```json
{
  "text": "string"
}
```

**Observed behavior**

- The controller constructs `new Document(request.text())`.
- It calls `vectorStore.add(List.of(doc))`.
- The configured vector store is pgvector-backed (`spring.ai.vectorstore.pgvector.*` in `application.yml`).
- Embedding generation is provided through the Ollama embedding model configured under `spring.ai.ollama.embedding.options.model`.

**Response**

```json
{
  "status": "ingested",
  "id": "string"
}
```

## `POST /api/rag/ask` (inactive)

**Status**  
Commented out in `src/main/java/com/pkm/agent/controller/RagController.java`.

**Observed code shape**

```java
/* @PostMapping("/ask")
public Map<String, String> ask(@RequestBody AskRequest request) {
    String answer = agentChatClient.prompt()
            .user(request.question())
            .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
            .call()
            .content();
    return Map.of("response", answer);
}*/
```

**Notes**

- The repository contains the code only as a comment block, so no active `/api/rag/ask` route is exposed at runtime.
- The `AskRequest` record exists in the controller source, but its endpoint is not enabled.

## External Interfaces and Configuration

These integrations are visible in the application code and configuration:

- Ollama chat model configured under `spring.ai.ollama.chat.*`
- Ollama embedding model configured under `spring.ai.ollama.embedding.*`
- pgvector-backed `VectorStore` configured under `spring.ai.vectorstore.pgvector.*`
- Local MCP server configured under `agent.mcp.local.*` and connected through SSE with Basic auth from config
- Parallel search MCP server configured under `agent.mcp.parallel-search.*` and connected through Streamable HTTP with API key from config

No concrete secrets are reproduced here.
