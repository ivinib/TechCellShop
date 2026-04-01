package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.controller.dto.request.UserUpdateRequest;
import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.repository.UserRepository;
import org.example.company.tcs.techcellshop.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setIdUser(1L);
        user.setNameUser("Ana Silva");
        user.setEmailUser("ana@techcellshop.com");
        user.setPasswordUser("senha123");
        user.setPhoneUser("+55 11 90000-0001");
        user.setAddressUser("Rua das Flores, 123 - Sao Paulo - SP");
        user.setRoleUser("USER");
    }

    @Test
    void saveUser_whenEmailDoesNotExist_shouldEncodePasswordAndReturnUserWithNullPassword() {
        when(userRepository.existsByEmailUserIgnoreCase(user.getEmailUser())).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = userService.saveUser(user);

        assertThat(result).isNotNull();
        assertThat(result.getPasswordUser()).isNull();
        verify(passwordEncoder).encode("senha123");
        verify(userRepository).save(user);
    }

    @Test
    void saveUser_whenEmailAlreadyExists_shouldThrowIllegalArgumentException() {
        when(userRepository.existsByEmailUserIgnoreCase(user.getEmailUser())).thenReturn(true);

        assertThatThrownBy(() -> userService.saveUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A user with this email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserById_whenUserExists_shouldReturnUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.getUserById(1L);

        assertThat(result.getIdUser()).isEqualTo(1L);
        assertThat(result.getNameUser()).isEqualTo("Ana Silva");
    }

    @Test
    void getUserById_whenUserNotFound_shouldThrowResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 99");
    }

    @Test
    void getAllUsers_shouldReturnAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<User> result = userService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getNameUser()).isEqualTo("Ana Silva");
    }

    @Test
    void getAllUsers_whenEmpty_shouldReturnEmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<User> result = userService.getAllUsers();

        assertThat(result).isEmpty();
    }

    @Test
    void updateUser_whenUserExists_shouldUpdateNameAndEmailAndReturn() {
        UserUpdateRequest updateRequest = new UserUpdateRequest();
        updateRequest.setNameUser("Ana Updated");
        updateRequest.setEmailUser("ana.updated@techcellshop.com");
        updateRequest.setPhoneUser("+55 11 90000-0001");
        updateRequest.setAddressUser("Rua das Flores, 123 - Sao Paulo - SP");
        updateRequest.setRoleUser("USER");

        User updatedUser = new User();
        updatedUser.setIdUser(1L);
        updatedUser.setNameUser("Ana Updated");
        updatedUser.setEmailUser("ana.updated@techcellshop.com");
        updatedUser.setPhoneUser("+55 11 90000-0001");
        updatedUser.setAddressUser("Rua das Flores, 123 - Sao Paulo - SP");
        updatedUser.setRoleUser("USER");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        User result = userService.updateUser(1L, updateRequest);

        assertThat(result.getNameUser()).isEqualTo("Ana Updated");
        assertThat(result.getEmailUser()).isEqualTo("ana.updated@techcellshop.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_whenUserNotFound_shouldThrowResourceNotFoundException() {
        UserUpdateRequest updateRequest = new UserUpdateRequest();
        updateRequest.setNameUser("Ana Updated");
        updateRequest.setEmailUser("ana.updated@techcellshop.com");
        updateRequest.setPhoneUser("+55 11 90000-0001");
        updateRequest.setAddressUser("Rua das Flores, 123 - Sao Paulo - SP");
        updateRequest.setRoleUser("USER");

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(99L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 99");
    }

    @Test
    void deleteUser_whenUserExists_shouldCallRepositoryDelete() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_whenUserNotFound_shouldThrowResourceNotFoundExceptionAndNeverDelete() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 99");

        verify(userRepository, never()).delete(any());
    }
}
