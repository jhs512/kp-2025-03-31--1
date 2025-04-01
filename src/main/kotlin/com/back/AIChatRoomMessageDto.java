package com.back;

import lombok.Getter;

@Getter
public class AIChatRoomMessageDto {
    private long id;
    private long chatRoomId;
    private String createDate;
    private String modifyDate;
    private String userMessage;
    private String botMessage;

    public AIChatRoomMessageDto(AIChatRoomMessage message) {
        this.id = message.getId();
        this.chatRoomId = message.getChatRoom().getId();
        this.createDate = message.getCreateDate().toString();
        this.modifyDate = message.getModifyDate().toString();
        this.userMessage = message.getUserMessage();
        this.botMessage = message.getBotMessage();
    }
}
