package com.aichatrestapi.clients.ssm;

import javax.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2", justification = "Handled via Dagger and safe by design")
@AllArgsConstructor(onConstructor_ = @Inject)
public class SsmClient {
    private final SecretsManagerClient client;

    public String getSecretValue(String secretName) {
        GetSecretValueRequest request = GetSecretValueRequest.builder().secretId(secretName).build();

        GetSecretValueResponse response = client.getSecretValue(request);
        return response.secretString();
    }
}
