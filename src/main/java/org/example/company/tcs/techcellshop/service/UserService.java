package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.controller.dto.request.UserUpdateRequest;
import org.example.company.tcs.techcellshop.domain.User;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface UserService {
    User saveUser(User user);
    User getUserById(Long id);
    List<User> getAllUsers();
    User updateUser(Long id, UserUpdateRequest request);
    void deleteUser(Long id);

}
