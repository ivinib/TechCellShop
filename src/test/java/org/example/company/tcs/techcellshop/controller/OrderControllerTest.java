package org.example.company.tcs.techcellshop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.company.tcs.techcellshop.config.SecurityConfig;
import org.example.company.tcs.techcellshop.controller.dto.request.OrderEnrollmentRequest;
import org.example.company.tcs.techcellshop.controller.dto.request.OrderUpdateRequest;
import org.example.company.tcs.techcellshop.controller.dto.response.DeviceSummaryResponse;
import org.example.company.tcs.techcellshop.controller.dto.response.OrderResponse;
import org.example.company.tcs.techcellshop.controller.dto.response.UserSummaryResponse;
import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.mapper.RequestMapper;
import org.example.company.tcs.techcellshop.mapper.ResponseMapper;
import org.example.company.tcs.techcellshop.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;


@SpringBootTest
@Import(SecurityConfig.class)
@DisplayName("OrderController")
class OrderControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private RequestMapper requestMapper;

    @MockitoBean
    private ResponseMapper responseMapper;

    private OrderEnrollmentRequest validRequest;
    private OrderUpdateRequest validUpdateRequest;
    private Order mockOrder;
    private OrderResponse mockOrderResponse;

    @BeforeEach
    void setUp() {
        this.mockMvc = webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        validRequest = new OrderEnrollmentRequest();
        validRequest.setIdUser(1L);
        validRequest.setIdDevice(1L);
        validRequest.setQuantityOrder(2);
        validRequest.setTotalPriceOrder(7999.80);
        validRequest.setStatusOrder("CREATED");
        validRequest.setOrderDate("2026-03-24");
        validRequest.setDeliveryDate("2026-03-31");
        validRequest.setPaymentMethod("CREDIT_CARD");
        validRequest.setPaymentStatus("PENDING");

        validUpdateRequest = new OrderUpdateRequest();
        validUpdateRequest.setQuantityOrder(2);
        validUpdateRequest.setStatusOrder("CREATED");
        validUpdateRequest.setDeliveryDate("2026-03-31");
        validUpdateRequest.setPaymentStatus("PENDING");

        User mockUser = new User();
        mockUser.setIdUser(1L);
        mockUser.setNameUser("Ana Silva");
        mockUser.setEmailUser("ana@techcellshop.com");

        Device mockDevice = new Device();
        mockDevice.setIdDevice(1L);
        mockDevice.setNameDevice("Galaxy S24");
        mockDevice.setDevicePrice(3999.90);

        mockOrder = new Order();
        mockOrder.setIdOrder(1L);
        mockOrder.setUser(mockUser);
        mockOrder.setDevice(mockDevice);
        mockOrder.setQuantityOrder(2);
        mockOrder.setTotalPriceOrder(7999.80);
        mockOrder.setStatusOrder("CREATED");
        mockOrder.setOrderDate("2026-03-24");
        mockOrder.setDeliveryDate("2026-03-31");
        mockOrder.setPaymentMethod("CREDIT_CARD");
        mockOrder.setPaymentStatus("PENDING");

        mockOrderResponse = new OrderResponse(
                1L,
                new UserSummaryResponse(1L, "Ana Silva", "a***a@techcellshop.com"),
                new DeviceSummaryResponse(1L, "Galaxy S24", 3999.90),
                2, 7999.80, "CREATED",
                "2026-03-24", "2026-03-31",
                "CREDIT_CARD", "PENDING"
        );
    }

    @Nested
    @DisplayName("POST /order")
    class SaveOrder {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 when request is valid")
        void shouldReturn200_whenRequestIsValid() throws Exception {
            when(requestMapper.toOrder(any())).thenReturn(mockOrder);
            when(orderService.saveOrder(any())).thenReturn(mockOrder);
            when(responseMapper.toOrderResponse(any())).thenReturn(mockOrderResponse);

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.idOrder").value(1L))
                    .andExpect(jsonPath("$.statusOrder").value("CREATED"))
                    .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"))
                    .andExpect(jsonPath("$.user.nameUser").value("Ana Silva"))
                    .andExpect(jsonPath("$.device.nameDevice").value("Galaxy S24"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when user id is null")
        void shouldReturn400_whenUserIdIsNull() throws Exception {
            validRequest.setIdUser(null);

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.idUser").exists());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when quantity is zero")
        void shouldReturn400_whenQuantityIsZero() throws Exception {
            validRequest.setQuantityOrder(0);

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.quantityOrder").exists());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when order status is invalid")
        void shouldReturn400_whenOrderStatusIsInvalid() throws Exception {
            validRequest.setStatusOrder("PENDING");

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusOrder").exists());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when date format is invalid")
        void shouldReturn400_whenDateFormatIsInvalid() throws Exception {
            validRequest.setOrderDate("24/03/2026");

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.orderDate").exists());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when payment method is invalid")
        void shouldReturn400_whenPaymentMethodIsInvalid() throws Exception {
            validRequest.setPaymentMethod("CASH");

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.paymentMethod").exists());
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void shouldReturn401_whenUnauthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /order")
    class GetAllOrders {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 with order list when authenticated")
        void shouldReturn200_withOrderList_whenAuthenticated() throws Exception {
            when(orderService.getAllOrders()).thenReturn(List.of(mockOrder));
            when(responseMapper.toOrderResponseList(any())).thenReturn(List.of(mockOrderResponse));

            mockMvc.perform(get("/api/v1/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].statusOrder").value("CREATED"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 200 with empty list when no orders exist")
        void shouldReturn200_withEmptyList_whenNoOrdersExist() throws Exception {
            when(orderService.getAllOrders()).thenReturn(List.of());
            when(responseMapper.toOrderResponseList(any())).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void shouldReturn401_whenUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/orders"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /order/{id}")
    class GetOrderById {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 when order is found")
        void shouldReturn200_whenOrderIsFound() throws Exception {
            when(orderService.getOrderById(1L)).thenReturn(mockOrder);
            when(responseMapper.toOrderResponse(any())).thenReturn(mockOrderResponse);

            mockMvc.perform(get("/api/v1/orders/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.idOrder").value(1L))
                    .andExpect(jsonPath("$.totalPriceOrder").value(7999.80));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when order is not found")
        void shouldReturn404_whenOrderIsNotFound() throws Exception {
            when(orderService.getOrderById(99L))
                    .thenThrow(new ResourceNotFoundException("Order not found with id: 99"));

            mockMvc.perform(get("/api/v1/orders/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Order not found with id: 99"));
        }
    }

    @Nested
    @DisplayName("PUT /order/{id}")
    class UpdateOrder {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 when order is updated")
        void shouldReturn200_whenOrderIsUpdated() throws Exception {
            OrderResponse updatedResponse = new OrderResponse(
                    1L,
                    new UserSummaryResponse(1L, "Ana Silva", "a***a@techcellshop.com"),
                    new DeviceSummaryResponse(1L, "Galaxy S24", 3999.90),
                    3, 11999.70, "PROCESSING",
                    "2026-03-24", "2026-03-31",
                    "CREDIT_CARD", "AUTHORIZED"
            );
            when(orderService.updateOrder(eq(1L), any())).thenReturn(mockOrder);
            when(responseMapper.toOrderResponse(any())).thenReturn(updatedResponse);

            mockMvc.perform(put("/api/v1/orders/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusOrder").value("PROCESSING"))
                    .andExpect(jsonPath("$.quantityOrder").value(3));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when order is not found")
        void shouldReturn404_whenOrderIsNotFound() throws Exception {
            when(orderService.updateOrder(eq(99L), any()))
                    .thenThrow(new ResourceNotFoundException("Order not found with id: 99"));

            mockMvc.perform(put("/api/v1/orders/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Order not found with id: 99"));
        }
    }

    @Nested
    @DisplayName("DELETE /order/{id}")
    class DeleteOrder {

        @Test
        @WithMockUser
        @DisplayName("Should return 204 when order is deleted")
        void shouldReturn204_whenOrderIsDeleted() throws Exception {
            doNothing().when(orderService).deleteOrder(1L);

            mockMvc.perform(delete("/api/v1/orders/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when order is not found")
        void shouldReturn404_whenOrderIsNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Order not found with id: 99"))
                    .when(orderService).deleteOrder(99L);

            mockMvc.perform(delete("/api/v1/orders/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Order not found with id: 99"));
        }
    }
}