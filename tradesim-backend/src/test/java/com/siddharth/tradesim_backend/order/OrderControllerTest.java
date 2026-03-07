package com.siddharth.tradesim_backend.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.model.dto.OrderRequest;
import com.siddharth.tradesim_backend.order.model.dto.OrderResponse;
import com.siddharth.tradesim_backend.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateOrder() throws Exception {

        UUID orderId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        OrderRequest request = new OrderRequest();

        ReflectionTestUtils.setField(request, "stockId", stockId);
        ReflectionTestUtils.setField(request, "quantity", 10);
        ReflectionTestUtils.setField(request, "side", OrderSide.BUY);
        ReflectionTestUtils.setField(request, "orderType", OrderType.LIMIT);
        ReflectionTestUtils.setField(request, "limitPrice", BigDecimal.valueOf(100));

        OrderResponse response = new OrderResponse(
                orderId,
                stockId,
                OrderSide.BUY,
                OrderType.LIMIT,
                OrderStatus.OPEN,
                10,
                10,
                BigDecimal.valueOf(100),
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
                        .with(authentication(auth))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.stockId").value(stockId.toString()))
                .andExpect(jsonPath("$.side").value(OrderSide.BUY.name()))
                .andExpect(jsonPath("$.orderType").value(OrderType.LIMIT.name()))
                .andExpect(jsonPath("$.status").value(OrderStatus.OPEN.name()))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.remainingQuantity").value(10))
                .andExpect(jsonPath("$.limitPrice").value(100));
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

        mockMvc.perform(delete("/orders/{orderId}/cancel", orderId).with(authentication(auth))).andExpect(status().isNoContent());
    }
}