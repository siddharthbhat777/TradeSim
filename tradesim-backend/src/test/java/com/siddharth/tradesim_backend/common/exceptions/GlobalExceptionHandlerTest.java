package com.siddharth.tradesim_backend.common.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unknownRouteShouldReturnNotFoundJson() throws Exception {
        mockMvc.perform(get("/this-route-does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("ENDPOINT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Endpoint not found."));
    }

    @Test
    void wrongMethodShouldReturnMethodNotAllowedJson() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void malformedJsonShouldReturnBadRequestJson() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "sid",
                                  "email": "sid@test.com",
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST_BODY"))
                .andExpect(jsonPath("$.message").value("Malformed JSON request body."));
    }
}