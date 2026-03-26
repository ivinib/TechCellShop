package org.example.company.tcs.techcellshop.controller;

import jakarta.validation.Valid;
import org.example.company.tcs.techcellshop.controller.dto.request.UserEnrollmentRequest;
import org.example.company.tcs.techcellshop.controller.dto.request.UserUpdateRequest;
import org.example.company.tcs.techcellshop.controller.dto.response.UserResponse;
import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.mapper.RequestMapper;
import org.example.company.tcs.techcellshop.mapper.ResponseMapper;
import org.example.company.tcs.techcellshop.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

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

    @PostMapping
    public ResponseEntity<UserResponse> saveUser(@Valid @RequestBody UserEnrollmentRequest request, UriComponentsBuilder uriBuilder) {
        User user = requestMapper.toUser(request);
        User savedUser = userService.saveUser(user);
        UserResponse response = responseMapper.toUserResponse(savedUser);

        URI location = uriBuilder
                .path("/api/v1/users/{id}")
                .buildAndExpand(savedUser.getIdUser())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(responseMapper.toUserResponseList(users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(responseMapper.toUserResponse(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        User updatedUser = userService.updateUser(id, request);
        return ResponseEntity.ok(responseMapper.toUserResponse(updatedUser));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> partiallyUpdateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        User updatedUser = userService.updateUser(id, request);
        return ResponseEntity.ok(responseMapper.toUserResponse(updatedUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
