package com.sapo2c.controller;

import com.sapo2c.dto.GraphDto.ChatRequest;
import com.sapo2c.dto.GraphDto.ChatResponse;
import com.sapo2c.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        ChatResponse response = chatService.processQuery(
            request.getMessage(),
            request.getHistory()
        );
        return ResponseEntity.ok(response);
    }
}
