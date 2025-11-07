package com.aienginerestapi.clients;

import com.aienginerestapi.clients.aichat.AIChatClient;
import com.aienginerestapi.clients.aichat.AIChatRequestException;
import com.aienginerestapi.clients.ssm.ParameterStoreClient;
import com.aienginerestapi.model.User;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AIChatUserManagementTest {
    private AIChatClient client;

    private static final String TEST_EMAIL = "myemail.gmail.com";
    private static final String TEST_PWD = "1234";

    @BeforeClass
    public void setup() {
        ParameterStoreClient parameterStoreClient = new ParameterStoreClient();
        String apiUrl =
                parameterStoreClient.getParameterValue("/aichat/backend/apiUrl");
        this.client = new AIChatClient(apiUrl);
    }

    @Test
    public void userLifeCycle_happy_path() {
        User user = User.builder()
                .email(TEST_EMAIL)
                .pwd(TEST_PWD)
                .build();
        deleteUserIfExists(user);

        client.callPutAddUser(user);

        client.callGetLoginUser(user);
        user.setPwd("abcd");
        client.callPostUpdateUser(user);
        client.callGetLoginUser(user);
        user.setPwd(TEST_PWD);
        AIChatRequestException ex = Assert.expectThrows(
                AIChatRequestException.class,
                () -> client.callPutAddUser(user)
        );
        Assert.assertTrue(ex.getMessage().contains("User already exists"));
        client.callDeleteRemoveUser(user);
    }

    @Test
    public void addUser_with_existing_email_should_fail() {
        User user = User.builder()
                .email(TEST_EMAIL)
                .pwd(TEST_PWD)
                .build();
        deleteUserIfExists(user);

        client.callPutAddUser(user);
        AIChatRequestException ex = Assert.expectThrows(
                AIChatRequestException.class,
                () -> client.callPutAddUser(user)
        );
        Assert.assertTrue(ex.getMessage().contains("User already exists"));
        client.callDeleteRemoveUser(user);
    }

    @Test
    public void addUser_no_password_should_fail() {
        User user = User.builder()
                .email(TEST_EMAIL)
                .build();
        AIChatRequestException ex = Assert.expectThrows(
                AIChatRequestException.class,
                () -> client.callPutAddUser(user)
        );
        Assert.assertTrue(ex.getMessage().contains("PWD is required"));
    }

    @Test
    public void addUser_no_email_should_fail() {
        User user = User.builder()
                .pwd(TEST_PWD)
                .build();

        AIChatRequestException ex = Assert.expectThrows(
                AIChatRequestException.class,
                () -> client.callPutAddUser(user)
        );
        Assert.assertTrue(ex.getMessage().contains("Email is required"));
    }

    @Test
    public void updateUser_no_email_should_fail() {
        User user = User.builder()
                .pwd(TEST_PWD)
                .build();

        AIChatRequestException ex = Assert.expectThrows(
                AIChatRequestException.class,
                () -> client.callPostUpdateUser(user)
        );
        Assert.assertTrue(ex.getMessage().contains("Email is required"));
    }

    @Test
    public void updateUser_user_does_not_exists_should_fail() {
        User user = User.builder()
                .email(TEST_EMAIL)
                .pwd(TEST_PWD)
                .build();
        deleteUserIfExists(user);
        AIChatRequestException ex = Assert.expectThrows(
                AIChatRequestException.class,
                () -> client.callPostUpdateUser(user)
        );
        Assert.assertTrue(ex.getMessage().contains("User does not exists"));
    }



    private void deleteUserIfExists(User user) {
        try {
            client.callDeleteRemoveUser(user);
        } catch (AIChatRequestException e) {
            // Deletion is performed defensively. If the user doesn't exist, it's ignored.
            // This ensures idempotency and avoids failures from previous partial executions.
            if (!e.getMessage().contains("User does not exists")) {
                throw e;
            }
        }
    }
}
