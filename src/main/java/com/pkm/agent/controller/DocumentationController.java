package com.pkm.agent.controller;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 * Drives the documentation agent: it scans the repository for Java elements that
 * lack Javadoc and rewrites the affected files with generated documentation using
 * the {@link DocumentationTools}.
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
public class DocumentationController {

    private static final String DOC_INSTRUCTION = """
            You are a Java documentation agent. Do the following, using the available tools:
            1. Call scanForMissingJavadoc to find public classes, methods and fields
               that have no Javadoc.
            2. For each reported file, call readJavaFile to get its current content.
            3. Add clear, accurate Javadoc comments to every undocumented public element.
               Do NOT change any code, imports, formatting or behaviour — only add
               documentation comments.
            4. Call writeJavaFile with the complete updated file content to save it.
            5. After all files are done, reply with a concise summary listing each file
               you documented and how many elements you added Javadoc to.
            """;

    private final ChatClient agentChatClient;

    public DocumentationController(ChatClient agentChatClient) {
        this.agentChatClient = agentChatClient;
    }

    /** Trigger a full repository documentation pass. */
    @PostMapping("/document")
    public Map<String, String> document() {
        log.info("Starting repository documentation pass");
        String summary = agentChatClient.prompt()
                .user(DOC_INSTRUCTION)
                .call()
                .content();
        log.info("Documentation pass complete");
        return Map.of("summary", summary);
    }
}
