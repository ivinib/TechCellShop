package org.example.company.tcs.techcellshop.controller;

import jakarta.validation.Valid;
import org.example.company.tcs.techcellshop.controller.dto.UserEnrollmentRequest;
import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.service.UserServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserServiceImpl userService;

    UserController(UserServiceImpl userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<User> saveUser(@Valid @RequestBody UserEnrollmentRequest request) {
        User user = new User();
        user.setNameUser(request.getNameUser());
        user.setEmailUser(request.getEmailUser());
        user.setPasswordUser(request.getPasswordUser());
        user.setPhoneUser(request.getPhoneUser());
        user.setAddressUser(request.getAddressUser());
        user.setRoleUser(request.getRoleUser());

        return userService.saveUser(user);
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        return userService.updateUser(id, user);
    }

     @DeleteMapping("/{id}")
     public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
         try {
             userService.getUserById(id);
             userService.deleteUser(id);
             return ResponseEntity.noContent().build();
         } catch (Exception e) {
             throw new RuntimeException(e.getMessage());
         }
     }
}
