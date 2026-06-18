package com.example.jhapcham.chat.application;


import com.example.jhapcham.chat.application.*;
import com.example.jhapcham.chat.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import lombok.RequiredArgsConstructor;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatService {

    @Value("${app.gemini.api-key:YOUR_API_KEY_HERE}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public String getChatResponse(ChatRequest request) {

        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
            throw new RuntimeException("Gemini API key is not configured in application.properties.");
        }

        String systemPromptText =
                "You are a helpful e-commerce assistant for the store 'Jhapcham'. " +
                        "The user is viewing this product:\n" +
                        request.getProductInfo() + "\n" +
                        "Answer only based on product details. Be concise, helpful, and friendly. " +
                        "If you don't know, say you don't know and suggest contacting the seller.";

        // Try preferred model first, then fallback models/endpoints for compatibility.
        List<String> candidateUrls = Arrays.asList(
                "https://generativelanguage.googleapis.com/v1/models/gemini-3.1-flash-lite:generateContent?key=" + apiKey,
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=" + apiKey,
                "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key=" + apiKey
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        List<Map<String, Object>> contents = new ArrayList<>();
        boolean hasUserMessage = false;

        // Chat history
        if (request.getMessages() != null) {
            for (ChatRequest.Message msg : request.getMessages()) {
                String role = "assistant".equals(msg.getRole()) ? "model" : "user";
                
                // Gemini expects the conversation history to start with a user message
                if ("model".equals(role) && !hasUserMessage) {
                    continue;
                }
                
                if ("user".equals(role)) {
                    hasUserMessage = true;
                }

                Map<String, Object> message = new HashMap<>();
                message.put("role", role);
                message.put("parts", Collections.singletonList(
                        Collections.singletonMap("text", msg.getContent())
                ));

                contents.add(message);
            }
        }

        // Current input with system prompt inlined to avoid "Unknown name systemInstruction" error
        String finalInput = "SYSTEM INSTRUCTIONS: " + systemPromptText + "\n\nUSER MESSAGE: " + request.getInput();

        Map<String, Object> currentUser = new HashMap<>();
        currentUser.put("role", "user");
        currentUser.put("parts", Collections.singletonList(
                Collections.singletonMap("text", finalInput)
        ));
        contents.add(currentUser);

        // Config
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 300);

        Map<String, Object> body = new HashMap<>();
        body.put("contents", contents);
        body.put("generationConfig", generationConfig);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        Exception lastError = null;
        for (String url : candidateUrls) {
            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
                String responseText = extractText(response);
                if (responseText != null && !responseText.trim().isEmpty()) {
                    return responseText;
                }
                lastError = new RuntimeException("Invalid response format from Gemini API.");
            } catch (HttpClientErrorException.NotFound e) {
                // Model/version unavailable on this endpoint; try the next candidate.
                lastError = e;
            } catch (Exception e) {
                lastError = e;
                break;
            }
        }

        throw new RuntimeException("Error communicating with Gemini API: " +
                (lastError != null ? lastError.getMessage() : "No compatible Gemini model endpoint available."), lastError);
    }

    private String extractText(ResponseEntity<Map> response) {
        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            return null;
        }

        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) response.getBody().get("candidates");

        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        if (content == null) {
            return null;
        }

        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) {
            return null;
        }

        return (String) parts.get(0).get("text");
    }
}
