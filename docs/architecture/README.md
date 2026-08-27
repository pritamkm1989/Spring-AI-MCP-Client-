# Spring AI MCP Client Architecture

## Overview

This repository contains a single Spring Boot application that exposes REST endpoints for:

- agent chat backed by Spring AI `ChatClient`
- repository documentation automation through local filesystem tools
- RAG document ingestion backed by pgvector

The application integrates with:

- a local Ollama instance for chat and embeddings
- two remote/local MCP servers with different authentication schemes
- PostgreSQL with pgvector
- the local repository filesystem for the documentation workflow

## Plan

1. Document the observed REST endpoints and mark inactive/commented-out code separately.
2. Capture system context, container, and component structure for the Spring Boot application.
3. Describe the key runtime flows: agent chat, repository documentation pass, and RAG ingestion.
4. Model the observed request/response records and stored document shape without inventing extra schema.
5. Record deployment/runtime dependencies and link every artifact from this index.

## Artifacts

- [API reference](api-reference.md) — endpoint inventory, request/response contracts, and implementation notes.
- [C4 context](diagrams/c4-context.mmd) — users, the Spring Boot system, and its external dependencies.
- [C4 container](diagrams/c4-container.mmd) — the deployable application container and connected services/stores.
- [C4 component](diagrams/c4-component.mmd) — controllers, configuration, tools, and cross-cutting logging inside the app.
- [Sequence: agent chat](diagrams/sequence-agent-chat.mmd) — request flow for `POST /api/agent/chat`.
- [Sequence: documentation pass](diagrams/sequence-documentation-pass.mmd) — request flow for `POST /api/agent/document`.
- [Sequence: RAG ingest](diagrams/sequence-rag-ingest.mmd) — request flow for `POST /api/rag/ingest`.
- [Entity/ER](diagrams/entity-er.mmd) — observed request/response records and stored document shape.
- [Data flow](diagrams/data-flow.mmd) — external actors, internal processes, data stores, and labeled flows.
- [Class diagram](diagrams/class-diagram.mmd) — key application classes and their relationships.
- [Deployment](diagrams/deployment.mmd) — runtime topology, ports, protocols, and stores.

## Notes

- `POST /api/rag/ask` is present only as commented-out code in `RagController`; it is not documented as an active endpoint.
- `GET /api/agent/tools` currently returns tool definitions from `mcpToolProvider` only, so the observed runtime output is limited to reachable MCP tools rather than all local `@Tool` methods.
- The repository does not define a JPA/domain entity model; the ER diagram therefore focuses on the API payload records and the stored Spring AI `Document` shape directly visible in the code.

## Source Files Used

- `README.md`
- `build.gradle`
- `src/main/resources/application.yml`
- `src/main/java/com/pkm/agent/AgentApplication.java`
- `src/main/java/com/pkm/agent/config/AgentConfig.java`
- `src/main/java/com/pkm/agent/config/McpConfig.java`
- `src/main/java/com/pkm/agent/controller/AgentController.java`
- `src/main/java/com/pkm/agent/controller/DocumentationController.java`
- `src/main/java/com/pkm/agent/controller/RagController.java`
- `src/main/java/com/pkm/agent/tools/AgentTools.java`
- `src/main/java/com/pkm/agent/tools/DocumentationTools.java`
- `src/main/java/com/pkm/agent/aspect/ChatModelLoggingAspect.java`
