package com.siddharth.tradesim_backend.auth;

import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.auth.model.dto.*;
import com.siddharth.tradesim_backend.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest("sid", "password");

        AuthTokenResult response = new AuthTokenResult("mock-jwt-accessToken", "mock-refresh-token", "sid", Role.USER);

        when(authService.loginUser(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-jwt-accessToken"))
                .andExpect(jsonPath("$.username").value("sid"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void shouldRegisterSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest("Siddharth Bhat", "sid", "sid@test.com", "Password@123", "HDFC Bank", "IN", null);

        RegisterResponse response = new RegisterResponse(
                UUID.randomUUID(),
                "Siddharth Bhat",
                "sid",
                "sid@test.com",
                "HDFC Bank",
                Role.USER,
                AccountStatus.ACTIVE
        );

        when(authService.registerUser(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Siddharth Bhat"))
                .andExpect(jsonPath("$.linkedBankName").value("HDFC Bank"))
                .andExpect(jsonPath("$.username").value("sid"))
                .andExpect(jsonPath("$.email").value("sid@test.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
    }

    @Test
    void malformedJsonShouldReturnBadRequest() throws Exception {
        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "fullName": "Siddharth Bhat",
                                          "username": "sid",
                                          "email": "sid@test.com",
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST_BODY"))
                .andExpect(jsonPath("$.message").value("Malformed JSON request body."));
    }

    @Test
    void validationFailureShouldReturnFieldErrors() throws Exception {
        RegisterRequest request = new RegisterRequest("", "", "bad-email", "weak", "", "", null);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.fullName").exists())
                .andExpect(jsonPath("$.fieldErrors.linkedBankName").exists())
                .andExpect(jsonPath("$.fieldErrors.username").value("Username is required"))
                .andExpect(jsonPath("$.fieldErrors.email").value("Invalid email format"))
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.countryCode").exists());
    }

    @Test
    void shouldReactivateSuccessfully() throws Exception {
        ReactivateRequest request = new ReactivateRequest("sid", "password");

        AuthTokenResult response = new AuthTokenResult(
                "mock-jwt-accessToken",
                "mock-refresh-token",
                "sid",
                Role.USER
        );

        when(authService.reactivateAccount(any(ReactivateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/reactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-jwt-accessToken"))
                .andExpect(jsonPath("$.username").value("sid"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void shouldDeactivateSuccessfully() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("sid").password("password").role(Role.USER).accountStatus(AccountStatus.ACTIVE).build();
        UserPrincipal principal = new UserPrincipal(user);

        DeactivateRequest request = new DeactivateRequest("password123");

        mockMvc.perform(post("/auth/deactivate")
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(authService).deactivateAccount(eq(userId), any(DeactivateRequest.class));
    }
}