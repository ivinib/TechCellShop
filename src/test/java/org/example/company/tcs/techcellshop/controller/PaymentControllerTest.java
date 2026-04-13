package org.example.company.tcs.techcellshop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import org.example.company.tcs.techcellshop.config.SecurityWebMvcTestConfig;
import org.example.company.tcs.techcellshop.dto.payment.PaymentActionRequestDto;
import org.example.company.tcs.techcellshop.dto.payment.PaymentResponseDto;
import org.example.company.tcs.techcellshop.exception.GlobalExceptionHandler;
import org.example.company.tcs.techcellshop.security.CustomUserDetailsService;
import org.example.company.tcs.techcellshop.security.JwtAuthenticationFilter;
import org.example.company.tcs.techcellshop.security.RestAccessDeniedHandler;
import org.example.company.tcs.techcellshop.security.RestAuthenticationEntryPoint;
import org.example.company.tcs.techcellshop.service.JwtService;
import org.example.company.tcs.techcellshop.service.PaymentService;
import org.example.company.tcs.techcellshop.util.PaymentStatus;
import org.example.company.tcs.techcellshop.util.TraceIdFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import({
        GlobalExceptionHandler.class,
        TraceIdFilter.class,
        SecurityWebMvcTestConfig.class
})
@DisplayName("PaymentController")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @MockitoBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    @MockitoBean
    private JwtService jwtService;

    private PaymentActionRequestDto validRequest() {
        PaymentActionRequestDto request = new PaymentActionRequestDto();
        request.setTransactionId("TXN-1001");
        request.setAmount(new BigDecimal("3999.90"));
        request.setReason("ok");
        return request;
    }

    private PaymentResponseDto response(Long orderId, PaymentStatus status) {
        PaymentResponseDto response = new PaymentResponseDto();
        response.setOrderId(orderId);
        response.setPaymentStatus(status);
        response.setProcessedAt(OffsetDateTime.now());
        return response;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/payments/orders/{id}/confirm should return 200")
    void confirm_shouldReturn200() throws Exception {
        when(paymentService.confirmPayment(eq(1L), any(PaymentActionRequestDto.class)))
                .thenReturn(response(1L, PaymentStatus.CONFIRMED));

        mockMvc.perform(post("/api/v1/payments/orders/1/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1L))
                .andExpect(jsonPath("$.paymentStatus").value("CONFIRMED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/payments/orders/{id}/fail should return 200")
    void fail_shouldReturn200() throws Exception {
        when(paymentService.failPayment(eq(1L), any(PaymentActionRequestDto.class)))
                .thenReturn(response(1L, PaymentStatus.FAILED));

        mockMvc.perform(post("/api/v1/payments/orders/1/fail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/payments/orders/{id}/refund should return 200")
    void refund_shouldReturn200() throws Exception {
        when(paymentService.refundPayment(eq(1L), any(PaymentActionRequestDto.class)))
                .thenReturn(response(1L, PaymentStatus.REFUNDED));

        mockMvc.perform(post("/api/v1/payments/orders/1/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("REFUNDED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/payments/orders/{id}/confirm should return 400 when amount is invalid")
    void confirm_shouldReturn400_whenInvalidPayload() throws Exception {
        PaymentActionRequestDto request = validRequest();
        request.setAmount(new BigDecimal("0.00"));

        mockMvc.perform(post("/api/v1/payments/orders/1/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors.amount").exists());
    }

    @Test
    @DisplayName("POST /api/v1/payments/orders/{id}/confirm should require authentication")
    void confirm_shouldReject_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/payments/orders/1/confirm")
                        .header("X-Trace-Id", "test-trace-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.traceId").value("test-trace-id"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void confirm_shouldReturn403_whenUserIsNotAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/payments/orders/1/confirm")
                        .header("X-Trace-Id", "test-trace-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.traceId").value("test-trace-id"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/orders/{id}/confirm should return 401 when token is malformed")
    void confirm_shouldReturn401_whenTokenIsMalformed() throws Exception {
        when(jwtService.extractUserName("not-a-valid-jwt"))
                .thenThrow(new JwtException("Invalid JWT"));

        mockMvc.perform(post("/api/v1/payments/orders/1/confirm")
                        .header("Authorization", "Bearer not-a-valid-jwt")
                        .header("X-Trace-Id", "test-trace-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.traceId").value("test-trace-id"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void confirm_shouldReturn400_whenAmountHasMoreThanTwoDecimalPlaces() throws Exception {
        PaymentActionRequestDto request = validRequest();
        request.setAmount(new BigDecimal("3999.999"));

        mockMvc.perform(post("/api/v1/payments/orders/1/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors.amount").exists());
    }
}