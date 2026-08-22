package com.siddharth.tradesim_backend.order;

import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.order.enums.FundingStrategy;
import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.enums.TimeInForce;
import com.siddharth.tradesim_backend.order.model.dto.OrderRequest;
import com.siddharth.tradesim_backend.order.model.dto.OrderResponse;
import com.siddharth.tradesim_backend.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    void shouldCreateOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        OrderRequest request = new OrderRequest(
                stockId,
                10,
                OrderSide.BUY,
                OrderType.LIMIT,
                TimeInForce.DAY,
                BigDecimal.valueOf(100),
                FundingStrategy.BASE
        );

        OrderResponse response = new OrderResponse(
                orderId,
                stockId,
                OrderSide.BUY,
                OrderType.LIMIT,
                TimeInForce.DAY,
                OrderStatus.OPEN,
                10,
                10,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(100),
                Instant.parse("2026-04-12T12:30:00Z"),
                null
        );

        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(user, "role", Role.USER);

        when(orderService.createOrder(any(UUID.class), any(OrderRequest.class))).thenReturn(response);

        UserPrincipal principal = new UserPrincipal(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(post("/orders/create")
                        .contentType(APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.stockId").value(stockId.toString()))
                .andExpect(jsonPath("$.side").value(OrderSide.BUY.name()))
                .andExpect(jsonPath("$.orderType").value(OrderType.LIMIT.name()))
                .andExpect(jsonPath("$.timeInForce").value(TimeInForce.DAY.name()))
                .andExpect(jsonPath("$.status").value(OrderStatus.OPEN.name()))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.remainingQuantity").value(10))
                .andExpect(jsonPath("$.limitPrice").value(100))
                .andExpect(jsonPath("$.bookPrice").value(100));
    }

    @Test
    void shouldCancelOrder() throws Exception {
        UUID orderId = UUID.randomUUID();

        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(user, "role", Role.USER);

        UserPrincipal principal = new UserPrincipal(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        doNothing().when(orderService).cancelOrder(any(UUID.class), any(UUID.class));

        mockMvc.perform(delete("/orders/{orderId}/cancel", orderId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isNoContent());
    }

    @Test
    void invalidOrderIdShouldReturnBadRequest() throws Exception {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(user, "role", Role.USER);

        UserPrincipal principal = new UserPrincipal(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(delete("/orders/not-a-uuid/cancel")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_PATH_VARIABLE"))
                .andExpect(jsonPath("$.fieldErrors.orderId").value("Expected UUID format."));
    }

    @Test
    void invalidEnumShouldReturnBadRequest() throws Exception {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(user, "role", Role.USER);

        UserPrincipal principal = new UserPrincipal(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(post("/orders/create")
                        .contentType(APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
                        .content("""
                                {
                                  "stockId": "%s",
                                  "quantity": 10,
                                  "side": "BUYING",
                                  "orderType": "LIMIT",
                                  "timeInForce": "DAY",
                                  "limitPrice": 100
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST_BODY"))
                .andExpect(jsonPath("$.message").value("Invalid value for 'side'. Allowed values are: BUY, SELL."));
    }

    @Test
    void invalidQuantityShouldReturnValidationError() throws Exception {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(user, "role", Role.USER);

        UserPrincipal principal = new UserPrincipal(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(post("/orders/create")
                        .contentType(APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
                        .content("""
                        {
                          "stockId": "%s",
                          "quantity": 0,
                          "side": "BUY",
                          "orderType": "LIMIT",
                          "timeInForce": "DAY",
                          "limitPrice": 100
                        }
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());
    }

    @Test
    void forbiddenCancelShouldReturnForbidden() throws Exception {
        UUID orderId = UUID.randomUUID();

        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(user, "role", Role.USER);

        UserPrincipal principal = new UserPrincipal(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        doThrow(OrderException.forbidden("You are not allowed to cancel this order"))
                .when(orderService)
                .cancelOrder(any(UUID.class), any(UUID.class));

        mockMvc.perform(delete("/orders/{orderId}/cancel", orderId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ORDER_FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("You are not allowed to cancel this order"));
    }
}