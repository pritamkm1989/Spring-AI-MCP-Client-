package com.pkm.agent.config;

import com.pkm.agent.tools.DocumentationTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.pkm.agent.tools.AgentTools;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class AgentConfig {

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
                               AgentTools agentTools,
                               DocumentationTools documentationTools,
                               ToolCallbackProvider mcpToolProvider,
                               ChatMemory chatMemory) {
        log.info("Building agent ChatClient with {} MCP tool callback(s)",
                mcpToolProvider.getToolCallbacks().length);
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(agentTools, documentationTools)
                .defaultToolCallbacks(mcpToolProvider.getToolCallbacks())
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor())
                .build();
    }
}
