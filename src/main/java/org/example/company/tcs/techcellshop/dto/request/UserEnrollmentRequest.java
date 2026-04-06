package org.example.company.tcs.techcellshop.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserEnrollmentRequest {

    @Schema(example = "Vinicius", description = "Name of the user")
    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 120, message = "Name must have between 3 and 120 characters")
    private String nameUser;

    @Schema(example = "email@techcellshop.com", description = "Email of the user")
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String emailUser;

    @Schema(example = "password123", description = "Password for the user account")
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 20, message = "Password must have between 6 and 20 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]+$", message = "Password must contain at least one letter and one number")
    private String passwordUser;

    @Schema(example = "(45) 99034-4234", description = "Phone number of the user, can include country code and special characters")
    @NotBlank(message = "Phone is required")
    @Pattern(
            regexp = "^\\+?[0-9\\-() ]{8,20}$",
            message = "Phone format is invalid"
    )
    private String phoneUser;

    @Schema(example = "Paulista Avenue, 235, Sao Paulo", description = "Address of the user, can include street, number, city")
    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 255, message = "Address must have between 5 and 255 characters")
    private String addressUser;

    @Schema(example = "USER",description = "Role of the user, must be either USER or ADMIN")
    @NotBlank(message = "Role is required")
    @Pattern(
            regexp = "^(USER|ADMIN)$",
            message = "Role must be either USER or ADMIN"
    )
    private String roleUser;
}
