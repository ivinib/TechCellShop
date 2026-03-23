package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<User> saveUser(User user) {
        try{
            if (userRepository.existsByEmailUserIgnoreCase(user.getEmailUser())) {
                throw new IllegalArgumentException("A user with this email already exists");
            }

            user.setPasswordUser(passwordEncoder.encode(user.getPasswordUser()));

            User savedUser = userRepository.save(user);
            log.info("User saved successfully");

            //Applying mask to the sensitive fields
            savedUser.setPasswordUser(null);
            savedUser.setEmailUser(maskEmail(savedUser.getEmailUser()));

            return ResponseEntity.ok(savedUser);
        } catch (IllegalArgumentException e){
            log.error("A user with email {} already exists", user.getEmailUser());
            throw e;
        } catch (Exception e) {
            log.error("An error occurred while trying to save the user. Error:" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public ResponseEntity<User> getUserById(Long id) {
        try{
            User user = userRepository.findById(id).orElse(null);
            if (user == null) {
                log.info("No user found with id {}", id);
                return ResponseEntity.notFound().build();
            }
            log.info("Returning user with id {}", id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("An error occurred while trying to get the user by id. Error:" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public ResponseEntity<List<User>> getAllUsers() {
        try{
            List<User> users = userRepository.findAll();
            if (users.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            log.info("Returning all users. Total of users found: {}", users.size());
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("An error occurred while trying to get all users. Error:" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public ResponseEntity<User> updateUser(Long id, User user) {
        try{
            User existingUser = userRepository.findById(id).orElse(null);
            if (existingUser == null) {
                return ResponseEntity.notFound().build();
            }

            existingUser.setNameUser(user.getNameUser());
            existingUser.setEmailUser(user.getEmailUser());
            User updatedUser = userRepository.save(existingUser);
            log.info("User with id {} updated successfully", id);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            log.error("An error occurred while trying to update the user. Error:" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public ResponseEntity<Void> deleteUser(Long id) {
        try{
            User existingUser = userRepository.findById(id).orElse(null);
            if (existingUser == null) {
                return ResponseEntity.notFound().build();
            }
            userRepository.delete(existingUser);
            log.info("User with id {} deleted successfully", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("An error occurred while trying to delete the user. Error:" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
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
