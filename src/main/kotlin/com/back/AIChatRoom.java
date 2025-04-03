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

        if (messages.size() > PREVIEWS_MESSAGES_COUNT && summaryMessages.isEmpty()) {
            AIChatRoomSummaryMessage summaryMessage = AIChatRoomSummaryMessage
                    .builder()
                    .chatRoom(this)
                    .message("0 ~ 2 요약")
                    .startMessageIndex(0)
                    .endMessageIndex(2)
                    .build();

            summaryMessages.add(summaryMessage);
        }

        return message;
    }
}
