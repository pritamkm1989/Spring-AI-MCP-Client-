package com.pkm.agent.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AgentToolsTest {

    private final AgentTools tools = new AgentTools();

    @Test
    void calculate_supportsAllArithmeticOperators() {
        assertThat(tools.calculate(8, "+", 2)).isEqualTo(10);
        assertThat(tools.calculate(8, "-", 2)).isEqualTo(6);
        assertThat(tools.calculate(8, "*", 2)).isEqualTo(16);
        assertThat(tools.calculate(8, "/", 2)).isEqualTo(4);
    }

    @Test
    void calculate_throwsForDivisionByZero() {
        assertThatThrownBy(() -> tools.calculate(8, "/", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Division by zero");
    }

    @Test
    void calculate_throwsForUnsupportedOperator() {
        assertThatThrownBy(() -> tools.calculate(8, "%", 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown operator: %");
    }
}
