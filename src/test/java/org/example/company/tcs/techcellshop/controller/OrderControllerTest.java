package org.example.company.tcs.techcellshop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.company.tcs.techcellshop.config.SecurityConfig;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.dto.order.OrderStatusUpdateRequestDto;
import org.example.company.tcs.techcellshop.dto.request.OrderEnrollmentRequest;
import org.example.company.tcs.techcellshop.dto.request.OrderUpdateRequest;
import org.example.company.tcs.techcellshop.dto.response.DeviceSummaryResponse;
import org.example.company.tcs.techcellshop.dto.response.OrderResponse;
import org.example.company.tcs.techcellshop.dto.response.UserSummaryResponse;
import org.example.company.tcs.techcellshop.exception.InsufficientStockException;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.mapper.ResponseMapper;
import org.example.company.tcs.techcellshop.service.OrderService;
import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.example.company.tcs.techcellshop.util.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@Import({SecurityConfig.class})
@DisplayName("OrderController")
class OrderControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private ResponseMapper responseMapper;

    private OrderEnrollmentRequest validEnrollmentRequest;
    private OrderUpdateRequest validUpdateRequest;
    private OrderStatusUpdateRequestDto statusUpdateRequest;
    private Order mockOrder;
    private OrderResponse mockOrderResponse;

    @BeforeEach
    void setUp() {
        this.mockMvc = webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        validEnrollmentRequest = new OrderEnrollmentRequest();
        validEnrollmentRequest.setIdUser(1L);
        validEnrollmentRequest.setIdDevice(1L);
        validEnrollmentRequest.setQuantityOrder(1);
        validEnrollmentRequest.setPaymentMethod("CREDIT_CARD");

        validUpdateRequest = new OrderUpdateRequest();
        validUpdateRequest.setQuantityOrder(1);
        validUpdateRequest.setStatusOrder(OrderStatus.PAID);
        validUpdateRequest.setDeliveryDate("2026-04-10");
        validUpdateRequest.setPaymentStatus(PaymentStatus.CONFIRMED);

        statusUpdateRequest = new OrderStatusUpdateRequestDto();
        statusUpdateRequest.setNewStatus(OrderStatus.SHIPPED);
        statusUpdateRequest.setReason("Sent to carrier");

        mockOrder = new Order();
        mockOrder.setIdOrder(1L);
        mockOrder.setQuantityOrder(1);
        mockOrder.setTotalPriceOrder(money("3999.90"));

        mockOrderResponse = new OrderResponse(
                1L,
                new UserSummaryResponse(1L, "Ana Silva", "a***a@techcellshop.com"),
                new DeviceSummaryResponse(1L, "Galaxy S24", money("3999.90")),
                1,
                money("3999.90"),
                OrderStatus.CREATED,
                "2026-04-01",
                "2026-04-06",
                "CREDIT_CARD",
                PaymentStatus.PENDING,
                "WELCOME10",
                money("10.00"),
                money("3989.90"),
                "User wants to pay with another card"
        );
    }

    @Nested
    @DisplayName("POST /api/v1/orders")
    class SaveOrder {

        @Test
        @WithMockUser
        @DisplayName("Should return 201 when request is valid")
        void shouldReturn201_whenRequestIsValid() throws Exception {
            when(orderService.placeOrder(any(OrderEnrollmentRequest.class), eq("KEY-001"))).thenReturn(mockOrder);
            when(responseMapper.toOrderResponse(mockOrder)).thenReturn(mockOrderResponse);

            mockMvc.perform(post("/api/v1/orders")
                            .header("Idempotency-Key", "KEY-001")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validEnrollmentRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/api/v1/orders/1")))
                    .andExpect(jsonPath("$.idOrder").value(1L))
                    .andExpect(jsonPath("$.statusOrder").value("CREATED"));
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void shouldReturn401_whenUnauthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/orders")
                            .header("Idempotency-Key", "KEY-001")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validEnrollmentRequest)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when Idempotency-Key is missing")
        void shouldReturn400_whenIdempotencyKeyIsMissing() throws Exception {
            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validEnrollmentRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                    .andExpect(jsonPath("$.path").value("/api/v1/orders"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 409 when stock is insufficient")
        void shouldReturn409_whenStockIsInsufficient() throws Exception {
            when(orderService.placeOrder(any(OrderEnrollmentRequest.class), eq("KEY-001")))
                    .thenThrow(new InsufficientStockException("Insufficient stock for device id 1"));

            mockMvc.perform(post("/api/v1/orders")
                            .header("Idempotency-Key", "KEY-001")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validEnrollmentRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("Conflict"))
                    .andExpect(jsonPath("$.code").value("BUSINESS_CONFLICT"))
                    .andExpect(jsonPath("$.message").value("Insufficient stock for device id 1"))
                    .andExpect(jsonPath("$.path").value("/api/v1/orders"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/orders")
    class GetAllOrders {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 with order list")
        void shouldReturn200_withOrderList() throws Exception {
            Page<Order> ordersPage = new PageImpl<>(List.of(mockOrder), PageRequest.of(0, 20), 1);

            when(orderService.getAllOrders(any(Pageable.class))).thenReturn(ordersPage);
            when(responseMapper.toOrderResponse(mockOrder)).thenReturn(mockOrderResponse);

            mockMvc.perform(get("/api/v1/orders")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].idOrder").value(1L))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.number").value(0))
                    .andExpect(jsonPath("$.size").value(20));
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void shouldReturn401_whenUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/orders"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/orders/{id}")
    class GetOrderById {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 when order is found")
        void shouldReturn200_whenOrderIsFound() throws Exception {
            when(orderService.getOrderById(1L)).thenReturn(mockOrder);
            when(responseMapper.toOrderResponse(any(Order.class))).thenReturn(mockOrderResponse);

            mockMvc.perform(get("/api/v1/orders/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.idOrder").value(1L));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when order is not found")
        void shouldReturn404_whenOrderIsNotFound() throws Exception {
            when(orderService.getOrderById(99L))
                    .thenThrow(new ResourceNotFoundException("Order not found with id: 99"));

            mockMvc.perform(get("/api/v1/orders/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("Order not found with id: 99"))
                    .andExpect(jsonPath("$.path").value("/api/v1/orders/99"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should include traceId in error response")
        void shouldIncludeTraceIdInErrorResponse() throws Exception {
            when(orderService.getOrderById(99L))
                    .thenThrow(new ResourceNotFoundException("Order not found with id: 99"));

            mockMvc.perform(get("/api/v1/orders/99")
                            .header("X-Trace-Id", "trace-test-123"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.traceId").value("trace-test-123"))
                    .andExpect(jsonPath("$.path").value("/api/v1/orders/99"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/orders/{id}")
    class UpdateOrder {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 when order is updated")
        void shouldReturn200_whenOrderIsUpdated() throws Exception {
            OrderResponse updatedResponse = new OrderResponse(
                    1L,
                    mockOrderResponse.user(),
                    mockOrderResponse.device(),
                    1,
                    money("3999.90"),
                    OrderStatus.PAID,
                    "2026-04-01",
                    "2026-04-10",
                    "CREDIT_CARD",
                    PaymentStatus.CONFIRMED,
                    "WELCOME10",
                    money("10.00"),
                    money("3989.90"),
                    "User wants to pay with another card"
            );

            when(orderService.updateOrder(eq(1L), any(OrderUpdateRequest.class))).thenReturn(mockOrder);
            when(responseMapper.toOrderResponse(any(Order.class))).thenReturn(updatedResponse);

            mockMvc.perform(put("/api/v1/orders/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusOrder").value("PAID"))
                    .andExpect(jsonPath("$.paymentStatus").value("CONFIRMED"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when order is not found")
        void shouldReturn404_whenOrderIsNotFound() throws Exception {
            when(orderService.updateOrder(eq(99L), any(OrderUpdateRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Order not found with id: 99"));

            mockMvc.perform(put("/api/v1/orders/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("Order not found with id: 99"))
                    .andExpect(jsonPath("$.path").value("/api/v1/orders/99"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/orders/{id}")
    class PartialUpdateOrder {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 when partially updated")
        void shouldReturn200_whenPartiallyUpdated() throws Exception {
            when(orderService.updateOrder(eq(1L), any(OrderUpdateRequest.class))).thenReturn(mockOrder);
            when(responseMapper.toOrderResponse(any(Order.class))).thenReturn(mockOrderResponse);

            mockMvc.perform(patch("/api/v1/orders/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.idOrder").value(1L));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/orders/{id}")
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
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("Order not found with id: 99"))
                    .andExpect(jsonPath("$.path").value("/api/v1/orders/99"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/orders/{id}/status")
    class UpdateStatus {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 when status is updated")
        void shouldReturn200_whenStatusIsUpdated() throws Exception {
            OrderResponse shippedResponse = new OrderResponse(
                    1L,
                    mockOrderResponse.user(),
                    mockOrderResponse.device(),
                    1,
                    money("3999.90"),
                    OrderStatus.SHIPPED,
                    "2026-04-01",
                    "2026-04-06",
                    "CREDIT_CARD",
                    PaymentStatus.CONFIRMED,
                    "WELCOME10",
                    money("10.00"),
                    money("3989.90"),
                    "User wants to pay with another card"
            );

            Order shippedOrder = new Order();
            shippedOrder.setIdOrder(1L);

            when(orderService.updateStatus(eq(1L), eq(OrderStatus.SHIPPED), eq("Sent to carrier")))
                    .thenReturn(shippedOrder);
            when(responseMapper.toOrderResponse(shippedOrder)).thenReturn(shippedResponse);

            mockMvc.perform(patch("/api/v1/orders/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusOrder").value("SHIPPED"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when newStatus is missing")
        void shouldReturn400_whenNewStatusIsMissing() throws Exception {
            OrderStatusUpdateRequestDto invalid = new OrderStatusUpdateRequestDto();
            invalid.setReason("missing status");

            mockMvc.perform(patch("/api/v1/orders/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.path").value("/api/v1/orders/1/status"))
                    .andExpect(jsonPath("$.validationErrors.newStatus").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/orders/{id}/cancel")
    class CancelOrder {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 when order is canceled")
        void shouldReturn200_whenOrderIsCanceled() throws Exception {
            OrderResponse canceledResponse = new OrderResponse(
                    1L,
                    mockOrderResponse.user(),
                    mockOrderResponse.device(),
                    1,
                    money("3999.90"),
                    OrderStatus.CANCELED,
                    "2026-04-01",
                    "2026-04-06",
                    "CREDIT_CARD",
                    PaymentStatus.FAILED,
                    "WELCOME10",
                    money("10.00"),
                    money("3989.90"),
                    "User wants to pay with another card"
            );

            Order canceledOrder = new Order();
            canceledOrder.setIdOrder(1L);

            when(orderService.cancelOrder(1L, "Customer request")).thenReturn(canceledOrder);
            when(responseMapper.toOrderResponse(canceledOrder)).thenReturn(canceledResponse);

            mockMvc.perform(post("/api/v1/orders/1/cancel")
                            .param("reason", "Customer request"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusOrder").value("CANCELED"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/orders/{id}/apply-coupon")
    class ApplyCoupon {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 when coupon is applied")
        void shouldReturn200_whenCouponIsApplied() throws Exception {
            OrderResponse discounted = new OrderResponse(
                    1L,
                    mockOrderResponse.user(),
                    mockOrderResponse.device(),
                    1,
                    money("3999.90"),
                    OrderStatus.CREATED,
                    "2026-04-01",
                    "2026-04-06",
                    "CREDIT_CARD",
                    PaymentStatus.PENDING,
                    "WELCOME10",
                    money("10.00"),
                    money("3989.90"),
                    "User wants to pay with another card"
            );

            Order discountedOrder = new Order();
            discountedOrder.setIdOrder(1L);

            when(orderService.applyCoupon(1L, "WELCOME10")).thenReturn(discountedOrder);
            when(responseMapper.toOrderResponse(discountedOrder)).thenReturn(discounted);

            mockMvc.perform(post("/api/v1/orders/1/apply-coupon")
                            .param("code", "WELCOME10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.idOrder").value(1L));
        }
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}