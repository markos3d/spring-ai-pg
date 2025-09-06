package dev.markos3d.spring_ai.record;

public record ChatRequest(String message) {
    public ChatRequest {
        if (message != null) {
            message = message.trim();
        }
    }
}