package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService{

    private UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User saveUser(User user) {
        if (userRepository.existsByEmailUserIgnoreCase(user.getEmailUser())) {
            log.error("A user with email {} already exists", user.getEmailUser());
            throw new IllegalArgumentException("A user with this email already exists");
        }

        user.setPasswordUser(passwordEncoder.encode(user.getPasswordUser()));
        User savedUser = userRepository.save(user);
        log.info("User saved successfully");

        savedUser.setPasswordUser(null);
        savedUser.setEmailUser(maskEmail(savedUser.getEmailUser()));

        return savedUser;
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.info("No user found with id {}", id);
                    return new ResourceNotFoundException("User not found with id: " + id);
                });
    }

    @Override
    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll();
        log.info("Returning all users. Total found: {}", users.size());
        return users;
    }

    @Override
    public User updateUser(Long id, User user) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.info("No user found with id {}", id);
                    return new ResourceNotFoundException("User not found with id: " + id);
                });

        existingUser.setNameUser(user.getNameUser());
        existingUser.setEmailUser(user.getEmailUser());
        User updatedUser = userRepository.save(existingUser);
        log.info("User with id {} updated successfully", id);
        return updatedUser;
    }

    @Override
    public void deleteUser(Long id) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.info("No user found with id {}", id);
                    return new ResourceNotFoundException("User not found with id: " + id);
                });

        userRepository.delete(existingUser);
        log.info("User with id {} deleted successfully", id);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];
        if (local.length() <= 2) return local.charAt(0) + "***@" + domain;
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + domain;
    }
}
