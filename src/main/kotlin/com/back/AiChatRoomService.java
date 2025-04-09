package com.back;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiChatRoomService {
    @Autowired
    @Lazy
    private AiChatRoomService self;
    private final AiChatRoomRepository aiChatRoomRepository;

    public Optional<AIChatRoom> findById(Long id) {
        return aiChatRoomRepository.findById(id);
    }

    public AIChatRoom makeNewRoom() {
        AIChatRoom aiChatRoom = AIChatRoom
                .builder()
                .systemMessage("""
                        당신은 한국인과 대화하고 있습니다.
                        한국의 문화와 정서를 이해하고 있어야 합니다.
                        최대한 한국어만 사용해야합니다.
                        """
                        .stripIndent()
                )
                .systemStrategyMessage("""
                        당신은 한국인과 대화하고 있습니다.
                        한국의 문화와 정서를 이해하고 있어야 합니다.
                        최대한 한국어만 사용해야합니다.
                        
                        아래 내용의 핵심을 요약해줘
                        """
                        .stripIndent()
                )
                .build();

        return aiChatRoomRepository.save(aiChatRoom);
    }

    public void addMessage(
            OpenAiChatModel chatClient,
            AIChatRoom aiChatRoom,
            String userMessage,
            String botMessage
    ) {
        if (aiChatRoom.needToMakeSummaryMessageOnNextMessageAdded()) {
            String newSummarySourceMessage = aiChatRoom.genNewSummarySourceMessage(userMessage, botMessage);

            String forSummaryUserMessage = """
                    %s
                    
                    %s
                    """
                    .formatted(
                            aiChatRoom.getSystemStrategyMessage(),
                            newSummarySourceMessage
                    )
                    .stripIndent();
            String forSummaryBotMessage = chatClient.call(forSummaryUserMessage);

            self._addMessage(aiChatRoom, userMessage, botMessage, forSummaryUserMessage, forSummaryBotMessage);
            return;
        }

        self._addMessage(aiChatRoom, userMessage, botMessage, null, null);
    }

    @Transactional
    public void _addMessage(AIChatRoom aiChatRoom, String userMessage, String botMessage, String forSummaryUserMessage, String forSummaryBotMessage) {
        AIChatRoomMessage aiChatRoomMessage = aiChatRoom.addMessage(
                userMessage,
                botMessage,
                aiChatRoom.getLastSummaryMessage()
        );

        if (forSummaryUserMessage != null && forSummaryBotMessage != null) {
            AIChatRoomSummaryMessage aiChatRoomSummaryMessage = aiChatRoom.addSummaryMessage(
                    forSummaryUserMessage,
                    forSummaryBotMessage
            );
        }

        aiChatRoomRepository.save(aiChatRoom);
    }
}