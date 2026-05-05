package com.example.st_meet;

public class ChatbotMessage {
    private String text;
    private boolean isUser;

    public ChatbotMessage(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
    }

    public String getText() {
        return text;
    }

    public boolean isUser() {
        return isUser;
    }
}
