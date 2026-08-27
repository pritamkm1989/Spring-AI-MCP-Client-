package com.pkm.agent.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.WebClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;

import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;

/**
 * Builds the MCP clients manually so each server can use its own authentication.
 * The auto-configuration shares a single WebClient across all connections, which
 * cannot carry per-server credentials.
 */
@Slf4j
@Configuration
public class McpConfig {

    /** Local MCP server over SSE, secured with HTTP Basic auth. */
    @Bean(destroyMethod = "close")
    @Lazy
    McpSyncClient localMcpClient(
            @Value("${agent.mcp.local.enabled:true}") boolean enabled,
            @Value("${agent.mcp.local.url}") String url,
            @Value("${agent.mcp.local.sse-endpoint}") String sseEndpoint,
            @Value("${agent.mcp.local.username}") String username,
            @Value("${agent.mcp.local.password}") String password) {

        if (!enabled) {
            log.info("Local MCP client disabled (agent.mcp.local.enabled=false), skipping");
            return null;
        }

        log.info("Initializing local MCP client url={} sseEndpoint={} username={}", url, sseEndpoint, username);
        String basicToken = HttpHeaders.encodeBasicAuth(username, password, StandardCharsets.UTF_8);
        WebClient.Builder webClientBuilder = WebClient.builder()
                .baseUrl(url)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basicToken);

        var transport = WebFluxSseClientTransport.builder(webClientBuilder)
                .sseEndpoint(sseEndpoint)
                .build();

        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(60))
                .build();
        return safeInitialize("local", client);
    }

    /** Parallel.ai search MCP over Streamable-HTTP, secured with an API key. */
    @Bean(destroyMethod = "close")
    @Lazy
    McpSyncClient parallelSearchMcpClient(
            @Value("${agent.mcp.parallel-search.enabled:true}") boolean enabled,
            @Value("${agent.mcp.parallel-search.url}") String url,
            @Value("${agent.mcp.parallel-search.endpoint}") String endpoint,
            @Value("${agent.mcp.parallel-search.api-key}") String apiKey) {

        if (!enabled) {
            log.info("Parallel-search MCP client disabled (agent.mcp.parallel-search.enabled=false), skipping");
            return null;
        }

        log.info("Initializing parallel-search MCP client url={} endpoint={}", url, endpoint);
        WebClient.Builder webClientBuilder = WebClient.builder()
                .baseUrl(url)
                .defaultHeader("x-api-key", apiKey);

        var transport = WebClientStreamableHttpTransport.builder(webClientBuilder)
                .endpoint(endpoint)
                .build();

        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(60))
                .build();
        return safeInitialize("parallel-search", client);
    }

    /**
     * Initializes an MCP client, but never lets an unreachable server abort startup.
     * On failure the client is closed and {@code null} is returned, so it is excluded
     * from the aggregated {@link List} and the agent degrades to the remaining tools.
     */
    McpSyncClient safeInitialize(String name, McpSyncClient client) {
        try {
            client.initialize();
            return client;
        } catch (Exception e) {
            log.warn("MCP server '{}' is unavailable, skipping its tools: {}", name, e.getMessage());
            try {
                client.close();
            } catch (Exception closeError) {
                log.debug("Failed to close unavailable MCP client '{}'", name, closeError);
            }
            return null;
        }
    }

    /** Aggregates the tools from all reachable MCP clients into one provider for the ChatClient. */
    @Bean
    @Lazy
    ToolCallbackProvider mcpToolProvider(List<McpSyncClient> mcpSyncClients) {
        log.info("Aggregating tools from {} reachable MCP client(s)", mcpSyncClients.size());
        return new SyncMcpToolCallbackProvider(mcpSyncClients);
    }
}
