package org.example.company.tcs.techcellshop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.company.tcs.techcellshop.config.SecurityConfig;
import org.example.company.tcs.techcellshop.dto.payment.PaymentActionRequestDto;
import org.example.company.tcs.techcellshop.dto.payment.PaymentResponseDto;
import org.example.company.tcs.techcellshop.service.PaymentService;
import org.example.company.tcs.techcellshop.util.PaymentStatus;
import org.example.company.tcs.techcellshop.util.TraceIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@Import(SecurityConfig.class)
@DisplayName("PaymentController")
class PaymentControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private PaymentService paymentService;

    private PaymentActionRequestDto validRequest;
    private PaymentResponseDto confirmedResponse;

    @BeforeEach
    void setUp() {
        this.mockMvc = webAppContextSetup(context)
                .addFilters(context.getBean(TraceIdFilter.class))
                .apply(springSecurity())
                .build();

        validRequest = new PaymentActionRequestDto();
        validRequest.setTransactionId("TXN-1001");
        validRequest.setAmount(new BigDecimal("3999.90"));
        validRequest.setReason("ok");

        confirmedResponse = new PaymentResponseDto();
        confirmedResponse.setOrderId(1L);
        confirmedResponse.setPaymentStatus(PaymentStatus.CONFIRMED);
        confirmedResponse.setProcessedAt(OffsetDateTime.now());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /payments/orders/{id}/confirm should return 200")
    void confirm_shouldReturn200() throws Exception {
        when(paymentService.confirmPayment(eq(1L), any(PaymentActionRequestDto.class)))
                .thenReturn(confirmedResponse);

        mockMvc.perform(post("/api/v1/payments/orders/1/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1L))
                .andExpect(jsonPath("$.paymentStatus").value("CONFIRMED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /payments/orders/{id}/fail should return 200")
    void fail_shouldReturn200() throws Exception {
        PaymentResponseDto failed = new PaymentResponseDto();
        failed.setOrderId(1L);
        failed.setPaymentStatus(PaymentStatus.FAILED);
        failed.setProcessedAt(OffsetDateTime.now());

        when(paymentService.failPayment(eq(1L), any(PaymentActionRequestDto.class)))
                .thenReturn(failed);

        mockMvc.perform(post("/api/v1/payments/orders/1/fail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /payments/orders/{id}/refund should return 200")
    void refund_shouldReturn200() throws Exception {
        PaymentResponseDto refunded = new PaymentResponseDto();
        refunded.setOrderId(1L);
        refunded.setPaymentStatus(PaymentStatus.REFUNDED);
        refunded.setProcessedAt(OffsetDateTime.now());

        when(paymentService.refundPayment(eq(1L), any(PaymentActionRequestDto.class)))
                .thenReturn(refunded);

        mockMvc.perform(post("/api/v1/payments/orders/1/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("REFUNDED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /payments/orders/{id}/confirm should return 400 when amount is invalid")
    void confirm_shouldReturn400_whenInvalidPayload() throws Exception {
        validRequest.setAmount(new BigDecimal("0.00"));

        mockMvc.perform(post("/api/v1/payments/orders/1/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.amount").exists());
    }

    @Test
    @DisplayName("POST /payments/orders/{id}/confirm should require authentication")
    void confirm_shouldReject_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/payments/orders/1/confirm")
                        .header("X-Trace-Id", "test-trace-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
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
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.traceId").value("test-trace-id"));
    }

    @Test
    @DisplayName("POST /payments/orders/{id}/confirm should return 401 when token is malformed")
    void confirm_shouldReturn401_whenTokenIsMalformed() throws Exception {
        mockMvc.perform(post("/api/v1/payments/orders/1/confirm")
                        .header("Authorization", "Bearer not-a-valid-jwt")
                        .header("X-Trace-Id", "test-trace-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.traceId").value("test-trace-id"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void confirm_shouldReturn400_whenAmountHasMoreThanTwoDecimalPlaces() throws Exception {
        validRequest.setAmount(new BigDecimal("3999.999"));

        mockMvc.perform(post("/api/v1/payments/orders/1/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.amount").exists());
    }
}