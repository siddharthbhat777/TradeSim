package com.siddharth.tradesim_backend.user;

import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.user.dto.ChangeUserRoleRequest;
import com.siddharth.tradesim_backend.user.dto.ChangeUserRoleResponse;
import com.siddharth.tradesim_backend.user.dto.ChangeUserStatusRequest;
import com.siddharth.tradesim_backend.user.dto.ChangeUserStatusResponse;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminShouldChangeUserStatus() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        User admin = User.builder()
                .id(adminId)
                .username("admin")
                .password("password")
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        UserPrincipal principal = new UserPrincipal(admin);

        ChangeUserStatusRequest request = new ChangeUserStatusRequest(AccountStatus.SUSPENDED);

        ChangeUserStatusResponse response = new ChangeUserStatusResponse(
                targetUserId,
                "test_user",
                "test@email.com",
                Role.USER,
                AccountStatus.SUSPENDED
        );

        when(userService.changeStatus(eq(targetUserId), eq(AccountStatus.SUSPENDED))).thenReturn(response);

        mockMvc.perform(
                        put("/users/change/{userId}/status", targetUserId)
                                .with(authentication(
                                        new UsernamePasswordAuthenticationToken(
                                                principal,
                                                null,
                                                principal.getAuthorities()
                                        )
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("SUSPENDED"))
                .andExpect(jsonPath("$.username").value("test_user"));
    }

    @Test
    void nonAdminShouldNotChangeUserStatus() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .username("sid")
                .password("password")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        UserPrincipal principal = new UserPrincipal(user);

        ChangeUserStatusRequest request = new ChangeUserStatusRequest(AccountStatus.BANNED);

        mockMvc.perform(
                        put("/users/change/{userId}/status", targetUserId)
                                .with(authentication(
                                        new UsernamePasswordAuthenticationToken(
                                                principal,
                                                null,
                                                principal.getAuthorities()
                                        )
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void adminShouldChangeUserRole() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        User admin = User.builder()
                .id(adminId)
                .username("admin")
                .password("password")
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        UserPrincipal principal = new UserPrincipal(admin);

        ChangeUserRoleRequest request = new ChangeUserRoleRequest(Role.COMPANY_REPRESENTATIVE);

        ChangeUserRoleResponse response = new ChangeUserRoleResponse(
                targetUserId,
                "normal1",
                "normal1@example.com",
                Role.COMPANY_REPRESENTATIVE,
                AccountStatus.ACTIVE
        );

        when(userService.changeRole(eq(targetUserId), eq(Role.COMPANY_REPRESENTATIVE))).thenReturn(response);

        mockMvc.perform(
                        put("/users/change/{userId}/role", targetUserId)
                                .with(authentication(
                                        new UsernamePasswordAuthenticationToken(
                                                principal,
                                                null,
                                                principal.getAuthorities()
                                        )
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("COMPANY_REPRESENTATIVE"))
                .andExpect(jsonPath("$.username").value("normal1"));
    }

    @Test
    void nonAdminShouldNotChangeUserRole() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .username("sid")
                .password("password")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        UserPrincipal principal = new UserPrincipal(user);

        ChangeUserRoleRequest request = new ChangeUserRoleRequest(Role.COMPANY_REPRESENTATIVE);

        mockMvc.perform(
                        put("/users/change/{userId}/role", targetUserId)
                                .with(authentication(
                                        new UsernamePasswordAuthenticationToken(
                                                principal,
                                                null,
                                                principal.getAuthorities()
                                        )
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void unknownUserShouldReturnNotFound() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        User admin = User.builder()
                .id(adminId)
                .username("admin")
                .password("password")
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        UserPrincipal principal = new UserPrincipal(admin);

        ChangeUserStatusRequest request = new ChangeUserStatusRequest(AccountStatus.SUSPENDED);

        when(userService.changeStatus(eq(targetUserId), eq(AccountStatus.SUSPENDED)))
                .thenThrow(UserException.notFound("User not found"));

        mockMvc.perform(
                        put("/users/change/{userId}/status", targetUserId)
                                .with(authentication(
                                        new UsernamePasswordAuthenticationToken(
                                                principal,
                                                null,
                                                principal.getAuthorities()
                                        )
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("User not found"));
    }
}