package com.back;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiChatRoomService {
    private final AiChatRoomRepository aiChatRoomRepository;

    public Optional<AIChatRoom> findById(Long id) {
        return aiChatRoomRepository.findById(id);
    }

    public AIChatRoom makeNewRoom() {
        AIChatRoom aiChatRoom = AIChatRoom.builder().build();
        return aiChatRoomRepository.save(aiChatRoom);
    }

    public void save(AIChatRoom aiChatRoom) {
        aiChatRoomRepository.save(aiChatRoom);
    }
}
