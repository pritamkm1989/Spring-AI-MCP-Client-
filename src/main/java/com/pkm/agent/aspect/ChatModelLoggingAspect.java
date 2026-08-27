package com.pkm.agent.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ChatModelLoggingAspect {

    @Around("execution(* org.springframework.ai.chat.model.ChatModel.call(..))")
    public Object logChatModel(ProceedingJoinPoint joinPoint) throws Throwable {

        Object[] args = joinPoint.getArgs();

        Prompt prompt = (Prompt) args[0];

        log.info(">>> CHAT REQUEST");
        prompt.getInstructions().forEach(msg ->
                log.info("[{}] {}", msg.getMessageType(), msg.getText())
        );

        long start = System.currentTimeMillis();

        ChatResponse response = (ChatResponse) joinPoint.proceed();

        long time = System.currentTimeMillis() - start;

        log.info("<<< CHAT RESPONSE ({} ms)", time);
        response.getResults().forEach(r ->
                log.info("Response: {}", r.getOutput().getText())
        );

        log.info("Prompt tokens: {}",
                response.getMetadata().getUsage().getPromptTokens());

        log.info("Completion tokens: {}",
                response.getMetadata().getUsage().getCompletionTokens());
        log.info("LLM  used: {}",
                response.getMetadata().getModel());

        return response;
    }
}
