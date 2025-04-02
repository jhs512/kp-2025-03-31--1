package com.back;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    @GetMapping(value = "/generateStream/{chatRoomId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public Flux<ServerSentEvent<String>> generateStream(
            @PathVariable Long chatRoomId,
            @RequestParam(value = "message", defaultValue = "Tell me a joke") String message
    ) {
        AIChatRoom aiChatRoom = aiChatRoomService.findById(chatRoomId).get();

        // 이전 대화 내용 가져오기 (최대 10개)
        List<Message> previousMessages = aiChatRoom.getMessages().stream()
                .limit(10)
                .flatMap(msg ->
                        List.of(
                                        new UserMessage(msg.getUserMessage()),
                                        new AssistantMessage(msg.getBotMessage())
                                )
                                .stream()
                )
                .collect(Collectors.toList());

        // 시스템 메시지 추가 (한국인 컨텍스트)
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage("당신은 한국인과 대화하고 있습니다. 한국의 문화와 정서를 이해하고 있어야 합니다."));
        messages.addAll(previousMessages);
        messages.add(new UserMessage(message));

        // 프롬프트 생성
        Prompt prompt = new Prompt(messages);
        StringBuilder fullResponse = new StringBuilder();

        // 스트리밍 처리
        return chatClient.stream(prompt)
                .map(chunk -> {
                    if (chunk.getResult() == null ||
                            chunk.getResult().getOutput() == null ||
                            chunk.getResult().getOutput().getText() == null) {

                        aiChatRoom.addMessage(
                                message,
                                fullResponse.toString()
                        );

                        aiChatRoomService.save(aiChatRoom);

                        return ServerSentEvent.<String>builder()
                                .data("[DONE]")
                                .build();
                    }

                    String text = chunk.getResult().getOutput().getText();
                    fullResponse.append(text);
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

    @GetMapping("/{chatRoomId}")
    public String room(
            @PathVariable Long chatRoomId,
            Model model
    ) {
        AIChatRoom aiChatRoom = aiChatRoomService.findById(chatRoomId).get();
        model.addAttribute("aiChatRoom", aiChatRoom);

        return "ai/chat/index";
    }

    @GetMapping("/{chatRoomId}/messages")
    @ResponseBody
    public List<AIChatRoomMessageDto> getMessages(
            @PathVariable Long chatRoomId
    ) {
        AIChatRoom aiChatRoom = aiChatRoomService.findById(chatRoomId).get();
        return aiChatRoom.getMessages()
                .stream()
                .map(AIChatRoomMessageDto::new)
                .toList();
    }
}
