package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.domain.User;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface UserService {
    ResponseEntity<User> saveUser(User user);

    ResponseEntity<User> getUserById(Long id);

    ResponseEntity<List<User>> getAllUsers();

    ResponseEntity<User> updateUser(Long id, User user);

    ResponseEntity<Void> deleteUser(Long id);

}
