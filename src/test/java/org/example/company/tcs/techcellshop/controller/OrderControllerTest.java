package org.example.company.tcs.techcellshop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.company.tcs.techcellshop.config.SecurityConfig;
import org.example.company.tcs.techcellshop.controller.dto.order.OrderStatusUpdateRequestDto;
import org.example.company.tcs.techcellshop.controller.dto.request.OrderEnrollmentRequest;
import org.example.company.tcs.techcellshop.controller.dto.request.OrderUpdateRequest;
import org.example.company.tcs.techcellshop.controller.dto.response.DeviceSummaryResponse;
import org.example.company.tcs.techcellshop.controller.dto.response.OrderResponse;
import org.example.company.tcs.techcellshop.controller.dto.response.UserSummaryResponse;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.mapper.RequestMapper;
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
        mockOrder.setTotalPriceOrder(3999.90);

        mockOrderResponse = new OrderResponse(
                1L,
                new UserSummaryResponse(1L, "Ana Silva", "a***a@techcellshop.com"),
                new DeviceSummaryResponse(1L, "Galaxy S24", 3999.90),
                1,
                3999.90,
                OrderStatus.CREATED,
                "2026-04-01",
                "2026-04-06",
                "CREDIT_CARD",
                PaymentStatus.PENDING
        );
    }

    @Nested
    @DisplayName("POST /api/v1/orders")
    class SaveOrder {

        @Test
        @WithMockUser
        @DisplayName("Should return 201 when request is valid")
        void shouldReturn201_whenRequestIsValid() throws Exception {
            when(orderService.placeOrder(any(OrderEnrollmentRequest.class))).thenReturn(mockOrder);
            when(responseMapper.toOrderResponse(any(Order.class))).thenReturn(mockOrderResponse);

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validEnrollmentRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/orders/1")))
                    .andExpect(jsonPath("$.idOrder").value(1L))
                    .andExpect(jsonPath("$.statusOrder").value("CREATED"));
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void shouldReturn401_whenUnauthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validEnrollmentRequest)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when payload is invalid")
        void shouldReturn400_whenPayloadIsInvalid() throws Exception {
            validEnrollmentRequest.setQuantityOrder(0);

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validEnrollmentRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/orders")
    class GetAllOrders {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 with order list")
        void shouldReturn200_withOrderList() throws Exception {
            when(orderService.getAllOrders()).thenReturn(List.of(mockOrder));
            when(responseMapper.toOrderResponseList(any())).thenReturn(List.of(mockOrderResponse));

            mockMvc.perform(get("/api/v1/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].idOrder").value(1L));
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void shouldReturn401_whenUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/orders"))
                    .andExpect(status().isForbidden());
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
                    .andExpect(jsonPath("$.message").value("Order not found with id: 99"));
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
                    3999.90,
                    OrderStatus.PAID,
                    "2026-04-01",
                    "2026-04-10",
                    "CREDIT_CARD",
                    PaymentStatus.CONFIRMED
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
                    .andExpect(jsonPath("$.message").value("Order not found with id: 99"));
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
                    .andExpect(jsonPath("$.message").value("Order not found with id: 99"));
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
                    3999.90,
                    OrderStatus.SHIPPED,
                    "2026-04-01",
                    "2026-04-06",
                    "CREDIT_CARD",
                    PaymentStatus.CONFIRMED
            );

            when(orderService.updateStatus(eq(1L), eq(OrderStatus.SHIPPED), eq("Sent to carrier")))
                    .thenReturn(shippedResponse);

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
                    .andExpect(jsonPath("$.message").value("Validation failed"));
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
                    3999.90,
                    OrderStatus.CANCELED,
                    "2026-04-01",
                    "2026-04-06",
                    "CREDIT_CARD",
                    PaymentStatus.FAILED
            );

            when(orderService.cancelOrder(1L, "Customer request")).thenReturn(canceledResponse);

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
                    3999.90,
                    OrderStatus.CREATED,
                    "2026-04-01",
                    "2026-04-06",
                    "CREDIT_CARD",
                    PaymentStatus.PENDING
            );

            when(orderService.applyCoupon(1L, "WELCOME10")).thenReturn(discounted);

            mockMvc.perform(post("/api/v1/orders/1/apply-coupon")
                            .param("code", "WELCOME10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.idOrder").value(1L));
        }
    }
}