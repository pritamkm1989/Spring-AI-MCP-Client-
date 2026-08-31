package com.pkm.agent.tools.base;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

public class RateLimitedToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final ToolRateLimiter rateLimiter;
    private final AgenticTool tool;
    private final ToolCallTracker tracker;

    public RateLimitedToolCallback(ToolCallback delegate, ToolRateLimiter rateLimiter,
                                   AgenticTool tool, ToolCallTracker tracker) {
        this.delegate = delegate;
        this.rateLimiter = rateLimiter;
        this.tool = tool;
        this.tracker = tracker;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        String name = getToolDefinition().name();
        if (!rateLimiter.allow(tool, name)) {
            return "Tool call limit reached for " + name
                    + " (max " + tool.getMaxCallsPerQuestion() + " per question)";
        }
        tracker.record(name, toolInput);
        return delegate.call(toolInput);
    }
}
