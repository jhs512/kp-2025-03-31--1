package com.back;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class AIChatRoomSummaryMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @CreatedDate
    private LocalDateTime createDate;
    @LastModifiedDate
    private LocalDateTime modifyDate;
    @ManyToOne
    private AIChatRoom chatRoom;
    @Column(columnDefinition = "LONGTEXT")
    private String userMessage;
    @Column(columnDefinition = "LONGTEXT")
    private String botMessage;
    private int startMessageIndex;
    private int endMessageIndex;

    public int getMessageNo() {
        return startMessageIndex + 1;
    }

    public int getEndMessageNo() {
        return endMessageIndex + 1;
    }
}
