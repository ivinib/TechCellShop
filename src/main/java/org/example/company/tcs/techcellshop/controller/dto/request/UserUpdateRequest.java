package org.example.company.tcs.techcellshop.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 120, message = "Name must have between 3 and 120 characters")
    private String nameUser;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String emailUser;

    @NotBlank(message = "Phone is required")
    @Pattern(
            regexp = "^\\+?[0-9\\-() ]{8,20}$",
            message = "Phone format is invalid"
    )
    private String phoneUser;

    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 255, message = "Address must have between 5 and 255 characters")
    private String addressUser;

    @NotBlank(message = "Role is required")
    @Pattern(
            regexp = "^(USER|ADMIN)$",
            message = "Role must be either USER or ADMIN"
    )
    private String roleUser;
}
