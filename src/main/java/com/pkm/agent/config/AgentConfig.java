package com.pkm.agent.config;

import com.pkm.agent.tools.DocumentationTools;
import com.pkm.agent.tools.base.AgenticTool;
import com.pkm.agent.tools.base.RateLimitedToolCallback;
import com.pkm.agent.tools.base.ToolCallTracker;
import com.pkm.agent.tools.base.ToolRateLimiter;
import lombok.AllArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@AllArgsConstructor
public class AgentConfig {

    private final ToolRateLimiter rateLimiter;
    private final ToolCallTracker toolCallTracker;

    private static final String SYSTEM_PROMPT = """
            You are an autonomous agent. You have access to local tools and remote MCP tools.
            Work towards the user's goal by reasoning step by step and calling the appropriate
            tools when they help. Prefer calling tools over guessing facts you cannot know.
            When you have used tools, briefly explain which tools you used and why.
            """;

    /** Short-term conversation memory keyed by conversationId. */
    @Bean
    ChatMemory chatMemory() {
        log.info("Configuring ChatMemory with maxMessages=20");
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }

    /**
     * The agent's ChatClient. It is wired with:
     *  - local @Tool methods (AgentTools)
     *  - all MCP server tools (auto-discovered via ToolCallbackProvider)
     *  - conversation memory + request logging advisors
     */
    @Bean
    @Lazy
    ChatClient agentChatClient(ChatClient.Builder builder,
                               List<AgenticTool> agentTools,
                               DocumentationTools documentationTools,
                               ObjectProvider<SyncMcpToolCallbackProvider> mcpToolProvider,
                               ChatMemory chatMemory) {


        Map<String, AgenticTool> toolMap = new HashMap<>();
        for (AgenticTool tool : agentTools) {
            ToolCallback[] cbs = ToolCallbacks.from(tool);
            for (ToolCallback cb : cbs) {
                toolMap.put(cb.getToolDefinition().name(), tool);
            }
        }

        List<ToolCallback> allCallbacks = new ArrayList<>();

        // Wrap @Tool annotated methods with rate limiting
        for (AgenticTool tool : agentTools) {
            ToolCallback[] cbs = ToolCallbacks.from(tool);
            for (ToolCallback cb : cbs) {
                allCallbacks.add(new RateLimitedToolCallback(cb, rateLimiter, tool, toolCallTracker));
            }
        }

        // Add MCP tool callbacks from Spring AI starter with cleaning + rate limiting
        SyncMcpToolCallbackProvider mcpProvider = mcpToolProvider.getIfAvailable();
        if (mcpProvider != null) {
            ToolCallback[] mcpCallbacks = mcpProvider.getToolCallbacks();
            if (mcpCallbacks != null && mcpCallbacks.length > 0) {
                log.info("[AgentToolExecutor] adding {} MCP tool callbacks from Spring AI starter", mcpCallbacks.length);
                AgenticTool mcpToolStub = new AgenticTool() {
                    @Override
                    public int getMaxCallsPerQuestion() {
                        return 5;
                    }
                };
                for (ToolCallback mcpCb : mcpCallbacks) {
                    allCallbacks.add(new RateLimitedToolCallback(mcpCb, rateLimiter, mcpToolStub, toolCallTracker));
                    log.info("[AgentToolExecutor]   + MCP tool: {}", mcpCb.getToolDefinition().name());
                }
            }
        }


        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultToolCallbacks(allCallbacks.toArray(ToolCallback[]::new))
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor())
                .build();

    }
}
