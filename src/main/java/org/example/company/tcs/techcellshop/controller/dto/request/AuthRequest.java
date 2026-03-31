package org.example.company.tcs.techcellshop.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        @Schema(example = "emailName@techcellshop.com", description = "User email used to authentication")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @Schema(example = "password123", description = "User password used to authentication")
        @NotBlank(message = "Password is required")
        String password
) { }
