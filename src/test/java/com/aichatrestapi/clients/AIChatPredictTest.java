package com.aichatrestapi.clients;

import com.aichatrestapi.clients.aichat.AIChatClient;
import com.aichatrestapi.clients.ssm.ParameterStoreClient;
import com.aichatrestapi.model.PredictRequestBody;
import com.aichatrestapi.model.PredictRequestMessage;
import com.aichatrestapi.model.PredictResponseBody;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.Assert;

import java.util.List;

public class AIChatPredictTest {
    private AIChatClient client;

    @BeforeClass
    public void setup() {
        ParameterStoreClient parameterStoreClient = new ParameterStoreClient();
        String token =
                parameterStoreClient.getParameterValue("/aichat/backend/apiUrl");
        this.client = new AIChatClient(token);
    }

    @Test(dataProviderClass = DataProviderClass.class, dataProvider = "messagesProvider")
    public void predict_happy_path(List<PredictRequestMessage> messages) {
        PredictResponseBody responseBody = client.callPostPredict(new PredictRequestBody(messages));
        Assert.assertFalse(responseBody.getMessage().isEmpty());
        Assert.assertFalse(responseBody.getMessage().contains("<think>"));
        Assert.assertFalse(responseBody.getMessage().contains("</think>"));
    }
}
