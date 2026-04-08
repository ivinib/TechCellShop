package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.dto.request.UserUpdateRequest;
import org.example.company.tcs.techcellshop.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface UserService {
    User saveUser(User user);
    User getUserById(Long id);
    Page<User> getAllUsers(Pageable pageable);
    User updateUser(Long id, UserUpdateRequest request);
    void deleteUser(Long id);

}
