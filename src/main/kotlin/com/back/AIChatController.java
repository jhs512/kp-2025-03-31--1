package com.back;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class AIChatController {
    private final OpenAiChatModel chatClient;

    @GetMapping("/generate")
    public String generate(
            @RequestParam(
                    value = "message",
                    defaultValue = "Tell me a joke"
            )
            String message
    ) {
        return chatClient
                .call(message);
    }

    @GetMapping(value = "/generateStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateStream(
            @RequestParam(value = "message", defaultValue = "Tell me a joke") String message
    ) {
        // 프롬프트 생성
        Prompt prompt = new Prompt(List.of(new UserMessage(message)));

        // 스트리밍 처리
        return chatClient.stream(prompt)
                .map(chunk -> chunk.getResult().getOutput().getText());
    }
}
