package com.aichatrestapi.clients.aichat;

import static com.aichatrestapi.TestConstants.*;
import static java.net.HttpURLConnection.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.aichatrestapi.Consts;
import com.aichatrestapi.clients.aichat.model.ChatSimpleMessageDO;
import com.aichatrestapi.clients.ssm.SsmClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ChatClientTest {

    // --- Tokens, roles y mensajes ---
    private static final String FAKE_TOKEN = "fake-token";
    private static final String BROKEN_MESSAGE = "broken";
    private static final String BAD_REQUEST = "Bad Request";
    private static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
    private static final String NO_ANSWER_MESSAGE = "No answer from chat engine";
    private static final String UNHANDLED_EXCEPTION = "unhandled exception";

    // --- Contenidos de respuesta JSON y streams ---
    private static final String RESPONSE_JSON = """
            {
              "id":"chatcmpl-abc123",
              "choices":[
                {
                  "index":0,
                  "finish_reason":"stop",
                  "message":{
                    "role":"assistant",
                    "content":"I need 6 bananas, 2 liters of milk, and a kilo of rice."
                  }
                }
              ],
              "model":"microsoft/phi-4",
              "object":"chat.completion"
            }
            """;

    private static final String EMPTY_CHOICES_JSON = """
            {
              "id":"chatcmpl-empty",
              "choices": [],
              "model":"microsoft/phi-4",
              "object":"chat.completion"
            }
            """;

    private static final String STREAM_DATA =
            "data: {\"id\":\"x\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"I n\"},"
                    + "\"finish_reason\":null}],\"model\":\"microsoft/phi-4\",\"object\":\"chat.completion.chunk\"}\n"
                    + "data: {\"id\":\"x\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"eed 6 bananas.\"},"
                    + "\"finish_reason\":\"stop\"}],\"model\":\"microsoft/phi-4\",\"object\":\"chat.completion"
                    + ".chunk\"}\n"
                    + "data: [DONE]\n";

    private static final String STREAM_EMPTY_CHOICES = """
            data: {"id":"x","choices":[],"model":"microsoft/phi-4","object":"chat.completion.chunk"}
            data: [DONE]
            """;

    private static final String STREAM_NULL_DELTA =
            "data: {\"id\":\"x\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\"},"
                    + "\"finish_reason\":null}],\"model\":\"model\",\"object\":\"chat.completion.chunk\"}\n"
                    + "data: [DONE]\n";


    private static final String STREAM_INVALID_LINE = """
            info: ignored
            data: [DONE]
            """;

    // --- Contenido esperado tras parseo ---
    private static final String EXPECTED_CONTENT = "I need 6 bananas, 2 liters of milk, and a kilo of rice.";
    private static final String PARTIAL_STREAM_CONTENT = "I need 6 bananas.";

    @Mock
    private HttpClient mockHttpClient;
    @Mock
    private SsmClient mockSsmClient;

    private AutoCloseable mocks;
    private AIChatClient AIChatClient;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        ObjectMapper objectMapper = new ObjectMapper();
        AIChatClient = new AIChatClient(mockSsmClient, mockHttpClient, objectMapper);
        when(mockSsmClient.getSecretValue(Consts.SSM_HUGGING_FACE_TOKEN))
                .thenReturn(FAKE_TOKEN);
    }

    @AfterEach
    void cleanup() throws Exception {
        mocks.close();
    }

    @Test
    void callChatEngine_nonStreaming_success() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(HTTP_OK);
        when(mockResponse.body()).thenReturn(RESPONSE_JSON);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        var result = AIChatClient.callChatEngine(
                List.of(ChatSimpleMessageDO.builder().role(USER_ROLE).content(CONTENT_HOLA).build()));

        assertEquals(ASSISTANT_ROLE, result.getRole());
        assertEquals(EXPECTED_CONTENT, result.getContent());
    }

    @Test
    void callChatEngine_streaming_success() throws Exception {
        ByteArrayInputStream stream = new ByteArrayInputStream(
                STREAM_DATA.getBytes(StandardCharsets.UTF_8));

        HttpResponse<InputStream> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(HTTP_OK);
        when(mockResponse.body()).thenReturn(stream);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        var result = AIChatClient.callChatEngineStream(
                List.of(ChatSimpleMessageDO.builder().role(USER_ROLE).content(CONTENT_HOLA).build()));

        assertEquals(ASSISTANT_ROLE, result.getRole());
        assertEquals(PARTIAL_STREAM_CONTENT, result.getContent());
    }

    @Test
    void callChatEngine_nonStreaming_httpError_throwsException() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(HTTP_BAD_REQUEST);
        when(mockResponse.body()).thenReturn(BAD_REQUEST);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        Exception exception = assertThrows(
                AIChatRequestException.class,
                () -> AIChatClient.callChatEngine(
                        List.of(ChatSimpleMessageDO.builder().role(USER_ROLE).content(CONTENT_HOLA).build())
                )
        );

        assertTrue(exception.getMessage().contains(String.valueOf(HTTP_BAD_REQUEST)));
        assertTrue(exception.getMessage().contains(BAD_REQUEST));
    }

    @Test
    void callChatEngine_streaming_httpError_throwsException() throws Exception {
        ByteArrayInputStream errorBody = new ByteArrayInputStream(
                INTERNAL_SERVER_ERROR.getBytes(Charset.defaultCharset()));

        HttpResponse<InputStream> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(HTTP_INTERNAL_ERROR);
        when(mockResponse.body()).thenReturn(errorBody);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        Exception exception = assertThrows(
                AIChatRequestException.class,
                () -> AIChatClient.callChatEngineStream(
                        List.of(ChatSimpleMessageDO.builder().role(USER_ROLE).content(CONTENT_HOLA).build())
                )
        );

        assertTrue(exception.getMessage().contains(String.valueOf(HTTP_INTERNAL_ERROR)));
        assertTrue(exception.getMessage().contains(INTERNAL_SERVER_ERROR));
    }

    @Test
    void callChatEngine_nonStreaming_emptyChoices_throwsException() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(HTTP_OK);
        when(mockResponse.body()).thenReturn(EMPTY_CHOICES_JSON);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        Exception exception = assertThrows(
                AIChatRequestException.class,
                () -> AIChatClient.callChatEngine(
                        List.of(ChatSimpleMessageDO.builder().role(USER_ROLE).content(CONTENT_HOLA).build())
                )
        );

        assertTrue(exception.getMessage().contains(NO_ANSWER_MESSAGE));
    }

    @Test
    void callChatEngine_streaming_emptyChoices_returnsEmptyMessage() throws Exception {
        ByteArrayInputStream stream = new ByteArrayInputStream(
                STREAM_EMPTY_CHOICES.getBytes(StandardCharsets.UTF_8));

        HttpResponse<InputStream> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(HTTP_OK);
        when(mockResponse.body()).thenReturn(stream);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        var result = AIChatClient.callChatEngineStream(
                List.of(ChatSimpleMessageDO.builder().role(USER_ROLE).content(CONTENT_HOLA).build()));

        assertEquals(ASSISTANT_ROLE, result.getRole());
        assertEquals(EMPTY_CONTENT, result.getContent());
    }

    @Test
    void callChatEngine_streaming_nullDeltaContent_isSkipped() throws Exception {
        ByteArrayInputStream stream = new ByteArrayInputStream(
                STREAM_NULL_DELTA.getBytes(StandardCharsets.UTF_8));

        HttpResponse<InputStream> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(HTTP_OK);
        when(mockResponse.body()).thenReturn(stream);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        var result = AIChatClient.callChatEngineStream(
                List.of(ChatSimpleMessageDO.builder().role(USER_ROLE).content(CONTENT_HOLA).build()));

        assertEquals(ASSISTANT_ROLE, result.getRole());
        assertEquals(EMPTY_CONTENT, result.getContent());
    }

    @Test
    void callChatEngine_streaming_ignoresInvalidLines() throws Exception {
        ByteArrayInputStream stream = new ByteArrayInputStream(
                STREAM_INVALID_LINE.getBytes(StandardCharsets.UTF_8));

        HttpResponse<InputStream> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(HTTP_OK);
        when(mockResponse.body()).thenReturn(stream);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        var result = AIChatClient.callChatEngineStream(
                List.of(ChatSimpleMessageDO.builder().role(USER_ROLE).content(CONTENT_HOLA).build()));

        assertEquals(ASSISTANT_ROLE, result.getRole());
        assertEquals(EMPTY_CONTENT, result.getContent());
    }

    @Test
    void callChatEngine_sendRequest_unexpectedException() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException(UNHANDLED_EXCEPTION));

        Exception exception = assertThrows(
                AIChatRequestException.class,
                () -> AIChatClient.callChatEngine(
                        List.of(ChatSimpleMessageDO.builder().role(USER_ROLE).content(CONTENT_HOLA).build())
                )
        );

        assertTrue(exception.getMessage().contains(UNHANDLED_EXCEPTION));
    }

    @Test
    void callChatEngine_buildHttpRequest_unexpectedException() throws Exception {
        ObjectMapper mockObjectMapper = mock(ObjectMapper.class);
        AIChatClient privateAIChatClient = new AIChatClient(mockSsmClient, mockHttpClient, mockObjectMapper);
        when(mockObjectMapper.writeValueAsString(any()))
                .thenThrow(new JsonMappingException(null, UNHANDLED_EXCEPTION));


        Exception exception = assertThrows(
                AIChatRequestException.class,
                () -> privateAIChatClient.callChatEngine(
                        List.of(ChatSimpleMessageDO.builder().role(USER_ROLE).content(CONTENT_HOLA).build())
                )
        );

        assertTrue(exception.getMessage().contains(UNHANDLED_EXCEPTION));
    }

    @Test
    void callChatEngineStream_buildHttpRequest_unexpectedException() throws Exception {
        ObjectMapper mockObjectMapper = mock(ObjectMapper.class);
        AIChatClient privateAIChatClient = new AIChatClient(mockSsmClient, mockHttpClient, mockObjectMapper);
        when(mockObjectMapper.writeValueAsString(any()))
                .thenThrow(new JsonMappingException(null, UNHANDLED_EXCEPTION));


        Exception exception = assertThrows(
                AIChatRequestException.class,
                () -> privateAIChatClient.callChatEngineStream(
                        List.of(ChatSimpleMessageDO.builder().role(USER_ROLE).content(CONTENT_HOLA).build())
                )
        );

        assertTrue(exception.getMessage().contains(UNHANDLED_EXCEPTION));
    }

    @Test
    void callChatEngine_nullInput_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> AIChatClient.callChatEngine(null));
    }

    @Test
    void callChatEngine_emptyInput_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> AIChatClient.callChatEngine(List.of()));
    }

    @Test
    void callChatEngineStream_nullInput_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> AIChatClient.callChatEngineStream(null));
    }

    @Test
    void callChatEngineStream_emptyInput_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> AIChatClient.callChatEngineStream(List.of()));
    }

    @Test
    void buildHttpRequest_jsonProcessingException_isWrappedInChatRequestException() throws Exception {
        ObjectMapper brokenMapper = mock(ObjectMapper.class);
        when(brokenMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException(BROKEN_MESSAGE) {
                });

        AIChatClient brokenClient = new AIChatClient(mockSsmClient, mockHttpClient, brokenMapper);

        Exception exception = assertThrows(
                AIChatRequestException.class,
                () -> brokenClient.callChatEngine(
                        List.of(ChatSimpleMessageDO.builder().role(USER_ROLE).content(CONTENT_HOLA).build())
                )
        );

        assertTrue(exception.getMessage().toLowerCase().contains(BROKEN_MESSAGE));
    }
}
