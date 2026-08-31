package com.pkm.agent.tools.base;

public interface AgenticTool {

    default int getMaxCallsPerQuestion() {
        return 5;
    }
}
