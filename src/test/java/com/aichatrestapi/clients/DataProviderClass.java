package com.aichatrestapi.clients;

import com.aichatrestapi.model.PredictRequestMessage;
import com.aichatrestapi.model.Roles;
import org.testng.annotations.DataProvider;

import java.util.List;

public class DataProviderClass {

    @DataProvider(name = "messagesProvider")
    public static Object[][] messagesProvider() {
        return new Object[][] {
                {List.of(new PredictRequestMessage(Roles.USER, "Hello"))},
                {List.of(new PredictRequestMessage(Roles.USER, "Hello"),
                        new PredictRequestMessage(Roles.ASSISTANT, "Hello!!!"),
                        new PredictRequestMessage(Roles.USER, "Nice to meet you"))},
        };
    }
}
