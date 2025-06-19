package com.aichatrestapi.clients;

import com.aichatrestapi.clients.aichat.AIChatClient;
import com.aichatrestapi.clients.ssm.ParameterStoreClient;
import com.aichatrestapi.model.PredictRequestBody;
import com.aichatrestapi.model.PredictRequestMessage;
import com.aichatrestapi.model.PredictResponseBody;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AIChatPredictTest {
    private AIChatClient client;

    private static final String HEADER_TOKEN = "X-HUGGING-FACE-TOKEN";
    private static final String HEADER_MODEL = "X-MODEL";
    private static final String HEADER_BASE_URL = "X-BASE-URL";
    private static final String HEADER_DO_TYPE = "X-DO-TYPE";

    @BeforeClass
    public void setup() {
        ParameterStoreClient parameterStoreClient = new ParameterStoreClient();
        String apiUrl =
                parameterStoreClient.getParameterValue("/aichat/backend/apiUrl");
        this.client = new AIChatClient(apiUrl);
    }

    @Test(dataProviderClass = DataProviderClass.class, dataProvider = "messagesProvider")
    public void predict_happy_path(List<PredictRequestMessage> messages) {
        PredictResponseBody responseBody = client.callPostPredict(new PredictRequestBody(messages));
        Assert.assertFalse(responseBody.getMessage().isEmpty());
        Assert.assertFalse(responseBody.getMessage().contains("<think>"));
        Assert.assertFalse(responseBody.getMessage().contains("</think>"));
    }

    @Test(dataProviderClass = DataProviderClass.class, dataProvider = "messagesProvider")
    public void predict_withHuggingFace_nebius_meta_llama_happy_path(List<PredictRequestMessage> messages) {

        Map<String, String> headers = new HashMap<>();
        headers.put(HEADER_BASE_URL, "https://router.huggingface.co/nebius/v1/chat/completions");
        headers.put(HEADER_MODEL, "meta-llama/Meta-Llama-3.1-8B-Instruct-fast");
        headers.put(HEADER_DO_TYPE, "HuggingFaceChatResponse");
        PredictResponseBody responseBody = client.callPostPredict(new PredictRequestBody(messages), headers);
        Assert.assertFalse(responseBody.getMessage().isEmpty());
        Assert.assertFalse(responseBody.getMessage().contains("<think>"));
        Assert.assertFalse(responseBody.getMessage().contains("</think>"));
    }
    @Test(dataProviderClass = DataProviderClass.class, dataProvider = "messagesProvider")

    public void predict_withHuggingFace_together_deepseek_r1_happy_path(List<PredictRequestMessage> messages) {

        Map<String, String> headers = new HashMap<>();
        headers.put(HEADER_BASE_URL, "https://router.huggingface.co/together/v1/chat/completions");
        headers.put(HEADER_MODEL, "deepseek-ai/DeepSeek-R1");
        headers.put(HEADER_DO_TYPE, "HuggingFaceChatResponse");
        PredictResponseBody responseBody = client.callPostPredict(new PredictRequestBody(messages), headers);
        Assert.assertFalse(responseBody.getMessage().isEmpty());
        Assert.assertFalse(responseBody.getMessage().contains("<think>"));
        Assert.assertFalse(responseBody.getMessage().contains("</think>"));
    }

}
