package org.example.company.tcs.techcellshop.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.company.tcs.techcellshop.domain.ErrorResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) throws IOException {
        String traceId = (String) request.getAttribute("traceId");
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                403,
                "Forbidden",
                "FORBIDDEN",
                "You do not have permission to access this resource",
                request.getRequestURI(),
                traceId,
                null
        );
        response.setStatus(403);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}