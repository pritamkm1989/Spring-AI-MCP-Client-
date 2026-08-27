package com.pkm.agent.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.Test;

import io.modelcontextprotocol.client.McpSyncClient;

class McpConfigTest {

    private final McpConfig config = new McpConfig();

    @Test
    void safeInitialize_returnsClient_whenInitializeSucceeds() {
        McpSyncClient client = mock(McpSyncClient.class);

        McpSyncClient result = config.safeInitialize("local", client);

        assertThat(result).isSameAs(client);
        verify(client).initialize();
        verifyNoMoreInteractions(client);
    }

    @Test
    void safeInitialize_returnsNullAndCloses_whenServerUnavailable() {
        McpSyncClient client = mock(McpSyncClient.class);
        doThrow(new RuntimeException("connection refused")).when(client).initialize();

        McpSyncClient result = config.safeInitialize("local", client);

        assertThat(result).isNull();
        verify(client).initialize();
        verify(client).close();
    }

    @Test
    void safeInitialize_returnsNull_evenWhenCloseAlsoFails() {
        McpSyncClient client = mock(McpSyncClient.class);
        doThrow(new RuntimeException("connection refused")).when(client).initialize();
        doThrow(new RuntimeException("close failed")).when(client).close();

        McpSyncClient result = config.safeInitialize("parallel-search", client);

        assertThat(result).isNull();
        verify(client).initialize();
        verify(client).close();
    }
}
