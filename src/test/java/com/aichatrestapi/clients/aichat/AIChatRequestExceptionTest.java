package com.aichatrestapi.clients.aichat;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

class AIChatRequestExceptionTest {
    @Test
    void constructor_withMessage_setsMessageCorrectly() {
        String errorMsg = "Error calling service";
        AIChatRequestException ex = new AIChatRequestException(errorMsg);

        assertEquals(errorMsg, ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void constructor_withMessageAndCause_setsFieldsCorrectly() {
        String errorMsg = "Network Failure";
        Throwable cause = new RuntimeException("timeout");

        AIChatRequestException ex = new AIChatRequestException(errorMsg, cause);

        assertEquals(errorMsg, ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}
