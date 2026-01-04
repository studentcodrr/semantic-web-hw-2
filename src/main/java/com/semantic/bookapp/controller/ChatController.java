package com.semantic.bookapp.controller;

import com.semantic.bookapp.model.ChatMessage;
import com.semantic.bookapp.model.ChatResponse;
import com.semantic.bookapp.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    // Handle chat messages
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatMessage message) {
        try {
            if (message.getMessage() == null || message.getMessage().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ChatResponse.error("Message cannot be empty"));
            }

            String response = chatService.generateResponse(
                    message.getMessage(),
                    message.getPageType(),
                    message.getBookId()
            );

            return ResponseEntity.ok(new ChatResponse(response));

        } catch (Exception e) {
            System.err.println("Error in chat endpoint: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(ChatResponse.error("An error occurred processing your message"));
        }
    }

    // Get context-aware conversation starters
    @GetMapping("/starters")
    public ResponseEntity<Map<String, Object>> getConversationStarters(
            @RequestParam(required = false) String pageType,
            @RequestParam(required = false) String bookId) {

        try {
            List<String> starters = chatService.generateConversationStarters(pageType, bookId);

            Map<String, Object> response = new HashMap<>();
            response.put("starters", starters);
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error getting starters: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}