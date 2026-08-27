package com.pkm.agent.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final ChatClient agentChatClient;
    private final ToolCallbackProvider mcpToolProvider;

    public AgentController(ChatClient agentChatClient, ToolCallbackProvider mcpToolProvider) {
        this.agentChatClient = agentChatClient;
        this.mcpToolProvider = mcpToolProvider;
    }

    public record ChatRequest(String message, String conversationId) {
    }

    /** Send a message to the agent. It may call local and/or MCP tools autonomously. */
    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody ChatRequest request) {
        String conversationId = (request.conversationId() == null || request.conversationId().isBlank())
                ? "default"
                : request.conversationId();

        log.info("Received chat request for conversationId={}", conversationId);

        String answer = agentChatClient.prompt()
                .user(request.message())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        log.debug("Completed chat response for conversationId={}", conversationId);
        return Map.of("conversationId", conversationId, "response", answer);
    }

    /** List every tool the agent can call (local + MCP), useful for debugging wiring. */
    @GetMapping("/tools")
    public List<Map<String, String>> tools() {
        log.info("Listing available agent tools");
        return Arrays.stream(mcpToolProvider.getToolCallbacks())
                .map(tc -> Map.of(
                        "name", tc.getToolDefinition().name(),
                        "description", tc.getToolDefinition().description()))
                .toList();
    }
}
