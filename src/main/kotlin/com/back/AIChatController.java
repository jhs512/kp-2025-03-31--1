package com.back;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@Controller
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class AIChatController {
    private final OpenAiChatModel chatClient;
    private final AiChatRoomService aiChatRoomService;

    @GetMapping("/generate")
    @ResponseBody
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
    @ResponseBody
    public Flux<ServerSentEvent<String>> generateStream(
            @RequestParam(value = "message", defaultValue = "Tell me a joke") String message
    ) {
        // 프롬프트 생성
        Prompt prompt = new Prompt(List.of(new UserMessage(message)));

        // 스트리밍 처리
        return chatClient.stream(prompt)
                .map(chunk -> {
                    if (chunk.getResult() == null ||
                            chunk.getResult().getOutput() == null ||
                            chunk.getResult().getOutput().getText() == null) {
                        return ServerSentEvent.<String>builder()
                                .data("[DONE]")
                                .build();
                    }

                    String text = chunk.getResult().getOutput().getText();
                    return ServerSentEvent.<String>builder()
                            .data("\"" + text + "\"")
                            .build();
                });
    }

    @GetMapping
    public String index() {
        AIChatRoom aiChatRoom = aiChatRoomService.makeNewRoom();

        return "redirect:/ai/chat/" + aiChatRoom.getId();
    }

    @GetMapping("/{id}")
    public String room(
            @PathVariable Long id,
            Model model
    ) {
        AIChatRoom aiChatRoom = aiChatRoomService.findById(id).get();
        model.addAttribute("aiChatRoom", aiChatRoom);

        return "ai/chat/index";
    }
}
