package com.pkm.agent.tools.base;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ToolRateLimiter {

    private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    public void reset() {
        counters.clear();
    }

    public boolean allow(AgenticTool tool, String toolName) {
        int max = tool.getMaxCallsPerQuestion();
        int used = counters.computeIfAbsent(toolName, k -> new AtomicInteger()).incrementAndGet();
        return used <= max;
    }
}
