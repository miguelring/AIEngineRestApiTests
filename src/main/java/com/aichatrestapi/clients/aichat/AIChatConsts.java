package com.aichatrestapi.clients.aichat;

public class AIChatConsts {
    public static final String BASE_URL = "https://0sqw8xb3w5.execute-api.eu-north-1.amazonaws.com/prod";

    public static final String PATH_PREDICT = "/predict";

    public static final String ERROR_MESSAGE_EMPTY_LIST = "Message list cannot be empty.";
    public static final String ERROR_MESSAGE_NO_ANSWER = "No answer from chat engine. - %s";
    public static final String ERROR_MESSAGE_CALLING_ENGINE = "Error when calling chat engine: %s %s";

    public static final String EMPTY_RESPONSE = "<empty>";
}
