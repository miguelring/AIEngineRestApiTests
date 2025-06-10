package com.aichatrestapi.clients.ssm;

import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

public class ParameterStoreClient {
    private final SsmClient client;

    public ParameterStoreClient() {
        client = SsmClient.create();
    }

    public String getParameterValue(String secretName) {
        GetParameterRequest request = GetParameterRequest.builder()
                .name(secretName)
                .withDecryption(true)
                .build();

        GetParameterResponse response = client.getParameter(request);
        return response.parameter().value();
    }
}
