package com.pkm.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class AgentApplication {

    public static void main(String[] args) {
        log.info("Starting AgentApplication...");
        SpringApplication.run(AgentApplication.class, args);
        log.info("AgentApplication started");
    }
}
