package com.back;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class AIChatRoom {
    public static final int PREVIEWS_MESSAGES_COUNT = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    private LocalDateTime createDate;

    @LastModifiedDate
    private LocalDateTime modifyDate;

    private String systemMessage;
    private String systemStrategyMessage;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AIChatRoomSummaryMessage> summaryMessages = new ArrayList<>();

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AIChatRoomMessage> messages = new ArrayList<>();

    public AIChatRoomMessage addMessage(String userMessage, String botMessage) {
        AIChatRoomMessage message = AIChatRoomMessage
                .builder()
                .chatRoom(this)
                .userMessage(userMessage)
                .botMessage(botMessage)
                .build();
        messages.add(message);

        return message;
    }

    // 다음 메세지가 추가된 후 요약을 새로할 필요가 있는지 체크
    public boolean needToMakeSummaryMessageOnNextMessageAdded() {
        if (messages.size() < PREVIEWS_MESSAGES_COUNT) {
            return false;
        }

        // 다다음 메세지가 생성되기 위해서 요약이 필요하다면
        // 다음 메세지가 생성된 직후 요약을 하는게 맞다.
        // 그렇기 때문에 여기서 다다음 메세지 번호를 기준으로 계산한다.
        int nextNextMessageNo = messages.size() + 2;

        // 판별공식 : 다다음_메세지_번호 - N > 마지막_요약_메세지_번호
        return nextNextMessageNo - PREVIEWS_MESSAGES_COUNT > getLastSummaryMessageEndMessageNo();
    }

    public int getLastSummaryMessageEndMessageIndex() {
        if (summaryMessages.isEmpty()) {
            return -1;
        }

        return summaryMessages.getLast().getEndMessageIndex();
    }

    public int getLastSummaryMessageEndMessageNo() {
        return getLastSummaryMessageEndMessageIndex() + 1;
    }

    public String genNewSummarySourceMessage(String userMessage, String botMessage) {
        StringBuilder messageBuilder = new StringBuilder();

        if (!summaryMessages.isEmpty()) {
            messageBuilder.append(summaryMessages.getLast().getBotMessage());
            messageBuilder.append("\n");
            messageBuilder.append("\n");
        }

        int startMessageIndex = getLastSummaryMessageEndMessageIndex() + 1;
        int endMessageIndex = getMessages().size() - 1;

        messageBuilder.append("== %d번 ~ %d번 내용 요약 ==".formatted(startMessageIndex, endMessageIndex + 1));
        messageBuilder.append("\n");

        for (int i = startMessageIndex; i <= endMessageIndex; i++) {
            AIChatRoomMessage message = messages.get(i);
            messageBuilder.append("Q: ").append(message.getUserMessage()).append("\n");
            messageBuilder.append("A: ").append(message.getBotMessage()).append("\n");
            messageBuilder.append("\n");
        }

        messageBuilder.append("Q: ").append(userMessage).append("\n");
        messageBuilder.append("A: ").append(botMessage).append("\n");
        messageBuilder.append("\n");

        return messageBuilder.toString();
    }

    public void addSummaryMessage(String forSummaryUserMessage, String forSummaryBotMessage) {
        AIChatRoomSummaryMessage summaryMessage = AIChatRoomSummaryMessage
                .builder()
                .chatRoom(this)
                .userMessage(forSummaryUserMessage)
                .botMessage(forSummaryBotMessage)
                .startMessageIndex(getLastSummaryMessageEndMessageIndex() + 1)
                .endMessageIndex(getMessages().size() - 1)
                .build();

        summaryMessages.add(summaryMessage);
    }
}
