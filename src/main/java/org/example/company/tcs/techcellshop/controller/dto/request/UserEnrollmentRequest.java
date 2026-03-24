package org.example.company.tcs.techcellshop.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserEnrollmentRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 120, message = "Name must have between 3 and 120 characters")
    private String nameUser;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String emailUser;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 20, message = "Password must have between 6 and 20 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]+$", message = "Password must contain at least one letter and one number")
    private String passwordUser;

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
