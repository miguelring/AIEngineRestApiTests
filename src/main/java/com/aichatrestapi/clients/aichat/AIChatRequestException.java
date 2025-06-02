package com.aichatrestapi.clients.aichat;

public class AIChatRequestException extends RuntimeException {

    public AIChatRequestException(String message) {
        super(message);
    }

    public AIChatRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
