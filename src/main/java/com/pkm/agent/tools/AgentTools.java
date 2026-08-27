package com.pkm.agent.tools;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Local tools exposed to the LLM. Any public method annotated with {@link Tool}
 * becomes callable by the agent alongside the remote MCP tools.
 */
@Slf4j
@Component
public class AgentTools {

    @Tool(description = "Get the current server date and time in ISO-8601 format")
    public String getCurrentDateTime() {
        log.info("Tool invoked: getCurrentDateTime");
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    @Tool(description = "Evaluate a simple arithmetic expression with two numbers. "
            + "Supported operators: +, -, *, /")
    public double calculate(double left, String operator, double right) {
        log.info("Tool invoked: calculate left={} operator={} right={}", left, operator, right);
        return switch (operator) {
            case "+" -> left + right;
            case "-" -> left - right;
            case "*" -> left * right;
            case "/" -> {
                if (right == 0) {
                    throw new IllegalArgumentException("Division by zero");
                }
                yield left / right;
            }
            default -> throw new IllegalArgumentException("Unknown operator: " + operator);
        };
    }

    @Tool(description = "Get basic information about the machine the agent is running on")
    public String getSystemInfo() {
        log.info("Tool invoked: getSystemInfo");
        return "OS=%s, arch=%s, javaVersion=%s, processors=%d".formatted(
                System.getProperty("os.name"),
                System.getProperty("os.arch"),
                System.getProperty("java.version"),
                Runtime.getRuntime().availableProcessors());
    }
}
