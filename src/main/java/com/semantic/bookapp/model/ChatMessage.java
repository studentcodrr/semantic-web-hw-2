package com.semantic.bookapp.model;

public class ChatMessage {
    private String message;
    private String context;
    private String pageType;
    private String bookId;
    private String userId;

    public ChatMessage() {
    }

    public ChatMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getPageType() {
        return pageType;
    }

    public void setPageType(String pageType) {
        this.pageType = pageType;
    }

    public String getBookId() {
        return bookId;
    }

    public String getUserId() {
        return userId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }
}