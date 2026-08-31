package com.pkm.agent.tools.base;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ToolCallTracker {

    private final ThreadLocal<List<ToolCall>> threadLocal = ThreadLocal.withInitial(ArrayList::new);

    public void record(String toolName, String input) {
        List<ToolCall> calls = threadLocal.get();
        calls.add(new ToolCall(toolName, input));
    }

    public List<ToolCall> getCalledTools() {
        return List.copyOf(threadLocal.get());
    }

    public List<String> getToolNames() {
        return threadLocal.get().stream()
                .map(ToolCall::name)
                .distinct()
                .toList();
    }

    public void reset() {
        threadLocal.remove();
    }

    public record ToolCall(String name, String input) {}
}
