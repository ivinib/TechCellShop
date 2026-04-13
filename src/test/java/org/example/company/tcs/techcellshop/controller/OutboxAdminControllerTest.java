package org.example.company.tcs.techcellshop.controller;

import org.example.company.tcs.techcellshop.config.SecurityWebMvcTestConfig;
import org.example.company.tcs.techcellshop.dto.OutboxEventResponseDto;
import org.example.company.tcs.techcellshop.dto.RequeueResultDto;
import org.example.company.tcs.techcellshop.exception.GlobalExceptionHandler;
import org.example.company.tcs.techcellshop.security.CustomUserDetailsService;
import org.example.company.tcs.techcellshop.security.JwtAuthenticationFilter;
import org.example.company.tcs.techcellshop.security.RestAccessDeniedHandler;
import org.example.company.tcs.techcellshop.security.RestAuthenticationEntryPoint;
import org.example.company.tcs.techcellshop.service.OutboxAdminService;
import org.example.company.tcs.techcellshop.util.TraceIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OutboxAdminController.class)
@Import({
        SecurityWebMvcTestConfig.class,
        GlobalExceptionHandler.class,
        TraceIdFilter.class
})
class OutboxAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OutboxAdminService outboxAdminService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @MockitoBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnFailedOutboxPage() throws Exception {
        Page<OutboxEventResponseDto> page = new PageImpl<>(List.of());
        when(outboxAdminService.listFailed(0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/outbox/failed"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403ForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/outbox/failed"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRequeueOne_whenAdmin() throws Exception {
        RequeueResultDto result = new RequeueResultDto(
                List.of(10L),
                1,
                List.of(),
                List.of(),
                "Requeue finished: 1 event(s) moved back to PENDING"
        );

        when(outboxAdminService.requeueOne(10L)).thenReturn(result);

        mockMvc.perform(post("/api/v1/admin/outbox/10/requeue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requeuedCount").value(1))
                .andExpect(jsonPath("$.requestedIds[0]").value(10));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRequeueBatch_whenAdmin() throws Exception {
        RequeueResultDto result = new RequeueResultDto(
                List.of(1L, 2L, 99L),
                2,
                List.of(99L),
                List.of(),
                "Requeue finished: 2 event(s) moved back to PENDING"
        );

        when(outboxAdminService.requeueFailed(List.of(1L, 2L, 99L))).thenReturn(result);

        mockMvc.perform(post("/api/v1/admin/outbox/requeue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": [1, 2, 99]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requeuedCount").value(2))
                .andExpect(jsonPath("$.notFoundIds[0]").value(99));
    }

    @Test
    void shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/admin/outbox/10/requeue"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn400_whenBatchPayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/admin/outbox/requeue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": []
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}