package com.back;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class AIChatController {
    @Autowired
    @Lazy
    private AIChatController self;
    private final OpenAiChatModel chatClient;
    private final AiChatRoomService aiChatRoomService;

    @GetMapping("/generate")
    @ResponseBody
    public String generate(
            @RequestParam(
                    defaultValue = "Tell me a joke"
            )
            String userMessage
    ) {
        return chatClient
                .call(userMessage);
    }

    @GetMapping(value = "/generateStream/{chatRoomId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    @Transactional(readOnly = true)
    public Flux<ServerSentEvent<String>> generateStream(
            @PathVariable Long chatRoomId,
            @RequestParam(defaultValue = "Tell me a joke") String userMessage
    ) {
        AIChatRoom aiChatRoom = aiChatRoomService.findById(chatRoomId).get();

        int lastSummaryMessageEndMessageIndex = aiChatRoom.getLastSummaryMessageEndMessageIndex();

        List<AIChatRoomMessage> oldMessages = aiChatRoom.getMessages();

        int oldMessagesToIndex = oldMessages.size() - 1;

        int oldMessagesFromIndex = Math.min(
                lastSummaryMessageEndMessageIndex + 1,
                oldMessagesToIndex - AIChatRoom.PREVIEWS_MESSAGES_COUNT
        );

        oldMessagesFromIndex = Math.max(0, oldMessagesFromIndex);

        // 이전 대화 내용 가져오기
        List<Message> previousMessages = oldMessages
                // 가장 마지막 요약 메시지 이후의 메시지들
                .subList(
                        oldMessagesFromIndex,
                        oldMessagesToIndex + 1
                )
                .stream()
                .flatMap(msg ->
                        Stream.of(
                                new UserMessage(msg.getUserMessage()),
                                new AssistantMessage(msg.getBotMessage())
                        )
                )
                .collect(Collectors.toList());

        // 시스템 메시지 추가 (한국인 컨텍스트)
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(aiChatRoom.getSystemMessage()));

        if (!aiChatRoom.getSummaryMessages().isEmpty()) {
            messages.add(
                    new SystemMessage(
                            aiChatRoom
                                    .getSummaryMessages()
                                    .getLast()
                                    .getBotMessage()
                    )
            );
        }

        messages.addAll(previousMessages);
        messages.add(new UserMessage(userMessage));

        // 프롬프트 생성
        Prompt prompt = new Prompt(messages);
        StringBuilder fullResponse = new StringBuilder();

        // 스트리밍 처리
        return chatClient.stream(prompt)
                .map(chunk -> {
                    if (chunk.getResult() == null ||
                            chunk.getResult().getOutput() == null ||
                            chunk.getResult().getOutput().getText() == null) {

                        String botMessage = fullResponse.toString();

                        self.addMessage(
                                aiChatRoom,
                                userMessage,
                                botMessage
                        );

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

    @Async
    public void addMessage(AIChatRoom aiChatRoom, String userMessage, String botMessage) {
        aiChatRoomService.addMessage(chatClient, aiChatRoom, userMessage, botMessage);
    }

    @GetMapping
    @Transactional
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
    @Transactional(readOnly = true)
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
