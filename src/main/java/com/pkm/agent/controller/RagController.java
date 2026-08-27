package com.pkm.agent.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * Retrieval-Augmented Generation endpoints backed by pgvector.
 * Ingest text, then ask questions that are answered using the stored context.
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final VectorStore vectorStore;
    private final ChatClient agentChatClient;

    public RagController(VectorStore vectorStore, ChatClient agentChatClient) {
        this.vectorStore = vectorStore;
        this.agentChatClient = agentChatClient;
    }

    public record IngestRequest(String text) {
    }

    public record AskRequest(String question) {
    }

    /** Store a piece of text (embedded via Ollama) into pgvector. */
    @PostMapping("/ingest")
    public Map<String, Object> ingest(@RequestBody IngestRequest request) {
        Document doc = new Document(request.text());
        vectorStore.add(List.of(doc));
        log.info("Ingested document id={}", doc.getId());
        return Map.of("status", "ingested", "id", doc.getId());
    }

    /** Answer a question grounded in the ingested documents. */
   /* @PostMapping("/ask")
    public Map<String, String> ask(@RequestBody AskRequest request) {
        String answer = agentChatClient.prompt()
                .user(request.question())
                .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .call()
                .content();
        return Map.of("response", answer);
    }*/
}
