package org.example.company.tcs.techcellshop.controller;

import org.example.company.tcs.techcellshop.config.JacksonConfig;
import org.example.company.tcs.testsupport.security.SecurityWebMvcTestConfig;
import org.example.company.tcs.techcellshop.dto.coupon.CouponValidationRequestDto;
import org.example.company.tcs.techcellshop.dto.coupon.CouponValidationResponseDto;
import org.example.company.tcs.techcellshop.exception.GlobalExceptionHandler;
import org.example.company.tcs.techcellshop.security.CustomUserDetailsService;
import org.example.company.tcs.techcellshop.security.JwtAuthenticationFilter;
import org.example.company.tcs.techcellshop.security.RestAccessDeniedHandler;
import org.example.company.tcs.techcellshop.security.RestAuthenticationEntryPoint;
import org.example.company.tcs.techcellshop.service.CouponService;
import org.example.company.tcs.techcellshop.service.JwtService;
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
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CouponController.class)
@Import({
        GlobalExceptionHandler.class,
        TraceIdFilter.class,
        SecurityWebMvcTestConfig.class,
        JacksonConfig.class,
})
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CouponService couponService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @MockitoBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    private CouponValidationRequestDto validRequest() {
        CouponValidationRequestDto request = new CouponValidationRequestDto();
        request.setCode("WELCOME10");
        request.setOrderAmount(new BigDecimal("3999.90"));
        return request;
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/coupons/validate should return 200 when valid")
    void validate_shouldReturn200_whenValid() throws Exception {
        CouponValidationResponseDto response = new CouponValidationResponseDto();
        response.setValid(true);
        response.setDiscountAmount(new BigDecimal("399.99"));
        response.setMessage("Coupon is valid");

        when(couponService.validateCoupon("WELCOME10", new BigDecimal("3999.90")))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/coupons/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.discountAmount").value(399.99));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/coupons/validate should return 400 when code is blank")
    void validate_shouldReturn400_whenCodeBlank() throws Exception {
        CouponValidationRequestDto request = validRequest();
        request.setCode(" ");

        mockMvc.perform(post("/api/v1/coupons/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors.code").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/coupons/validate should return 400 when amount is invalid")
    void validate_shouldReturn400_whenAmountInvalid() throws Exception {
        CouponValidationRequestDto request = validRequest();
        request.setOrderAmount(new BigDecimal("0.00"));

        mockMvc.perform(post("/api/v1/coupons/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors.orderAmount").exists());
    }

    @Test
    @DisplayName("POST /api/v1/coupons/validate should require authentication")
    void validate_shouldReject_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/coupons/validate")
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
    @WithMockUser
    void validate_shouldReturn400_whenAmountHasMoreThanTwoDecimalPlaces() throws Exception {
        CouponValidationRequestDto request = validRequest();
        request.setOrderAmount(new BigDecimal("3999.999"));

        mockMvc.perform(post("/api/v1/coupons/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors.orderAmount").exists());
    }
}