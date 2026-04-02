package org.example.company.tcs.techcellshop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.company.tcs.techcellshop.config.SecurityConfig;
import org.example.company.tcs.techcellshop.controller.dto.coupon.CouponValidationRequestDto;
import org.example.company.tcs.techcellshop.controller.dto.coupon.CouponValidationResponseDto;
import org.example.company.tcs.techcellshop.service.CouponService;
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

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@Import(SecurityConfig.class)
@DisplayName("CouponController")
class CouponControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private CouponService couponService;

    private CouponValidationRequestDto validRequest;

    @BeforeEach
    void setUp() {
        this.mockMvc = webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        validRequest = new CouponValidationRequestDto();
        validRequest.setCode("WELCOME10");
        validRequest.setOrderAmount(new BigDecimal("3999.90"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /coupons/validate should return 200 when valid")
    void validate_shouldReturn200_whenValid() throws Exception {
        CouponValidationResponseDto response = new CouponValidationResponseDto();
        response.setValid(true);
        response.setDiscountAmount(new BigDecimal("399.99"));
        response.setMessage("Coupon is valid");

        when(couponService.validateCoupon("WELCOME10", new BigDecimal("3999.90")))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/coupons/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.discountAmount").value(399.99));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /coupons/validate should return 400 when code is blank")
    void validate_shouldReturn400_whenCodeBlank() throws Exception {
        validRequest.setCode(" ");

        mockMvc.perform(post("/api/v1/coupons/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.code").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /coupons/validate should return 400 when amount is invalid")
    void validate_shouldReturn400_whenAmountInvalid() throws Exception {
        validRequest.setOrderAmount(new BigDecimal("0.00"));

        mockMvc.perform(post("/api/v1/coupons/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.orderAmount").exists());
    }

    @Test
    @DisplayName("POST /coupons/validate should require authentication")
    void validate_shouldReject_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/coupons/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }
}