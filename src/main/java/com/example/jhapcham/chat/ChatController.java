package com.example.jhapcham.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/product")
    public ResponseEntity<?> getChatResponse(@RequestBody ChatRequest request) {
        try {
            String aiResponse = chatService.getChatResponse(request);
            Map<String, String> response = new HashMap<>();
            response.put("reply", aiResponse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
