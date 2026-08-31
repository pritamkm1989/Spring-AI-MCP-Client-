package com.pkm.agent.controller;

import com.pkm.agent.tools.base.ToolCallTracker;
import com.pkm.agent.tools.base.ToolRateLimiter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/agent")
@AllArgsConstructor
public class AgentController {

    private final ChatClient agentChatClient;
    private final ToolRateLimiter rateLimiter;
    private final ToolCallTracker toolCallTracker;


    public record ChatRequest(String message, String conversationId) {
    }

    /** Send a message to the agent. It may call local and/or MCP tools autonomously. */
    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody ChatRequest request) {
        rateLimiter.reset();
        toolCallTracker.reset();
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
        List<String> trackedTools = toolCallTracker.getToolNames();
        log.info("[AgentToolExecutor] tracked tools: {}", trackedTools);
        return Map.of("conversationId", conversationId, "response", answer);
    }

}
