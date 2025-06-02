package com.aichatrestapi.clients.aichat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.aichatrestapi.clients.Consts;
import com.aichatrestapi.model.PredictRequestBody;
import com.aichatrestapi.model.PredictResponseBody;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor()
public class AIChatClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PredictResponseBody callPostPredict(PredictRequestBody inputMessages) {
        if (inputMessages == null || inputMessages.getMessages().isEmpty()) {
            throw new IllegalArgumentException(AIChatConsts.ERROR_MESSAGE_EMPTY_LIST);
        }

        try {
            HttpRequest request = buildHttpRequest(inputMessages, AIChatConsts.PATH_PREDICT);
            String body = sendRequest(request, HttpResponse.BodyHandlers.ofString());

            PredictResponseBody parsed = objectMapper.readValue(body, PredictResponseBody.class);
            if (parsed.getMessage().isEmpty()) {
                throw new AIChatRequestException(AIChatConsts.ERROR_MESSAGE_NO_ANSWER.formatted(body));
            }

            return parsed;
        } catch (IOException e) {
            throw new AIChatRequestException(AIChatConsts.ERROR_MESSAGE_CALLING_ENGINE.formatted(e.getMessage(), ""));
        }
    }

    private HttpRequest buildHttpRequest(PredictRequestBody requestPayload, String path) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(requestPayload);

        return HttpRequest.newBuilder().uri(URI.create(AIChatConsts.BASE_URL+ path))
                .header(Consts.HEADER_CONTENT_TYPE, Consts.CONTENT_TYPE_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
    }

    private <T> T sendRequest(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
        try {
            HttpResponse<T> response = httpClient.send(request, handler);

            if (response.statusCode() != 200) {
                String errorBody = AIChatConsts.EMPTY_RESPONSE;
                if (response.body() instanceof InputStream is) {
                    errorBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                } else if (response.body() != null) {
                    errorBody = response.body().toString();
                }

                throw new AIChatRequestException(
                        AIChatConsts.ERROR_MESSAGE_CALLING_ENGINE.formatted(response.statusCode(), errorBody));
            }

            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new AIChatRequestException(AIChatConsts.ERROR_MESSAGE_CALLING_ENGINE.formatted(e.getMessage(), ""),
                    e);
        }
    }
}
