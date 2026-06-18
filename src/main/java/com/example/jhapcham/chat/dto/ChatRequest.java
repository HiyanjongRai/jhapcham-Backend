package com.example.jhapcham.chat.dto;


import com.example.jhapcham.chat.application.*;
import com.example.jhapcham.chat.dto.*;
import lombok.Data;
import java.util.List;

@Data
public class ChatRequest {
    private String productInfo;
    private List<Message> messages;
    private String input;

    @Data
    public static class Message {
        private String role;
        private String content;
    }
}
