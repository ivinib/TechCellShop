package org.example.company.tcs.techcellshop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.company.tcs.techcellshop.dto.request.UserEnrollmentRequest;
import org.example.company.tcs.techcellshop.dto.request.UserUpdateRequest;
import org.example.company.tcs.techcellshop.dto.response.UserResponse;
import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.mapper.RequestMapper;
import org.example.company.tcs.techcellshop.mapper.ResponseMapper;
import org.example.company.tcs.techcellshop.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

import static org.example.company.tcs.techcellshop.util.AppConstants.SECURITY_SCHEME_NAME;

@Tag(name = "User Management", description = "User management endpoints")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final RequestMapper requestMapper;
    private final ResponseMapper responseMapper;

    public UserController(UserService userService, RequestMapper requestMapper, ResponseMapper responseMapper) {
        this.userService = userService;
        this.requestMapper = requestMapper;
        this.responseMapper = responseMapper;
    }

    @Operation(
            summary = "Enroll a new user to database",
            description = "Creates a new user account"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    @PostMapping
    public ResponseEntity<UserResponse> saveUser(
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User enrollment payload",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "User payload example",
                                    value = """
                                            {
                                              "nameUser": "Ana Silva",
                                              "emailUser": "ana@techcellshop.com",
                                              "passwordUser": "123456",
                                              "phoneUser": "+55 11 90000-0001",
                                              "addressUser": "Sao Paulo - SP",
                                              "roleUser": "CUSTOMER"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody UserEnrollmentRequest request,
            UriComponentsBuilder uriBuilder) {
        User user = requestMapper.toUser(request);
        User savedUser = userService.saveUser(user);
        UserResponse response = responseMapper.toUserResponse(savedUser);

        URI location = uriBuilder
                .path("/api/v1/users/{id}")
                .buildAndExpand(savedUser.getIdUser())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            summary = "List all users",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(responseMapper.toUserResponseList(users));
    }

    @Operation(
            summary = "Get a specific user using its id",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(responseMapper.toUserResponse(user));
    }

    @Operation(
            summary = "Update an user",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        User updatedUser = userService.updateUser(id, request);
        return ResponseEntity.ok(responseMapper.toUserResponse(updatedUser));
    }

    @Operation(
            summary = "Partially update an user",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> partiallyUpdateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        User updatedUser = userService.updateUser(id, request);
        return ResponseEntity.ok(responseMapper.toUserResponse(updatedUser));
    }

    @Operation(
            summary = "Delete an user based on its id",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
