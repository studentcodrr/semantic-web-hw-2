package com.semantic.bookapp.model;

import java.util.List;

public class ChatResponse {
    private String response;
    private boolean success;
    private String error;
    private List<String> conversationStarters;

    public ChatResponse() {
        this.success = true;
    }

    public ChatResponse(String response) {
        this.response = response;
        this.success = true;
    }

    public ChatResponse(String response, List<String> conversationStarters) {
        this.response = response;
        this.conversationStarters = conversationStarters;
        this.success = true;
    }

    public static ChatResponse success(String response) {
        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setResponse(response);
        chatResponse.setSuccess(true);
        return chatResponse;
    }

    public static ChatResponse error(String error) {
        ChatResponse response = new ChatResponse();
        response.setSuccess(false);
        response.setError(error);
        return response;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public List<String> getConversationStarters() {
        return conversationStarters;
    }

    public void setConversationStarters(List<String> conversationStarters) {
        this.conversationStarters = conversationStarters;
    }
}