package org.example.company.tcs.techcellshop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.company.tcs.techcellshop.config.SecurityConfig;
import org.example.company.tcs.techcellshop.controller.dto.request.UserEnrollmentRequest;
import org.example.company.tcs.techcellshop.controller.dto.response.UserResponse;
import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.mapper.RequestMapper;
import org.example.company.tcs.techcellshop.mapper.ResponseMapper;
import org.example.company.tcs.techcellshop.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import org.springframework.web.context.WebApplicationContext;


@SpringBootTest
@Import(SecurityConfig.class)
@DisplayName("UserController")
class UserControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RequestMapper requestMapper;

    @MockitoBean
    private ResponseMapper responseMapper;

    private UserEnrollmentRequest validRequest;
    private User mockUser;
    private UserResponse mockUserResponse;

    @BeforeEach
    void setUp() {
        this.mockMvc = webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        validRequest = new UserEnrollmentRequest();
        validRequest.setNameUser("Ana Silva");
        validRequest.setEmailUser("ana@techcellshop.com");
        validRequest.setPasswordUser("senha123");
        validRequest.setPhoneUser("+55 11 90000-0001");
        validRequest.setAddressUser("Rua das Flores, 123 - Sao Paulo - SP");
        validRequest.setRoleUser("USER");

        mockUser = new User();
        mockUser.setIdUser(1L);
        mockUser.setNameUser("Ana Silva");
        mockUser.setEmailUser("ana@techcellshop.com");
        mockUser.setPhoneUser("+55 11 90000-0001");
        mockUser.setAddressUser("Rua das Flores, 123 - Sao Paulo - SP");
        mockUser.setRoleUser("USER");

        mockUserResponse = new UserResponse(
                1L, "Ana Silva", "a***a@techcellshop.com",
                "+55 11 90000-0001", "Rua das Flores, 123 - Sao Paulo - SP", "USER"
        );
    }

    @Nested
    @DisplayName("POST /user")
    class SaveUser {

        @Test
        @DisplayName("Should return 200 when request is valid")
        void shouldReturn200_whenRequestIsValid() throws Exception {
            when(requestMapper.toUser(any())).thenReturn(mockUser);
            when(userService.saveUser(any())).thenReturn(mockUser);
            when(responseMapper.toUserResponse(any())).thenReturn(mockUserResponse);

            mockMvc.perform(post("/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.idUser").value(1L))
                    .andExpect(jsonPath("$.nameUser").value("Ana Silva"))
                    .andExpect(jsonPath("$.emailUserMasked").value("a***a@techcellshop.com"))
                    .andExpect(jsonPath("$.roleUser").value("USER"));
        }

        @Test
        @DisplayName("Should return 400 when name is too short")
        void shouldReturn400_whenNameIsTooShort() throws Exception {
            validRequest.setNameUser("Al");

            mockMvc.perform(post("/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.nameUser").exists());
        }

        @Test
        @DisplayName("Should return 400 when email is invalid")
        void shouldReturn400_whenEmailIsInvalid() throws Exception {
            validRequest.setEmailUser("not-an-email");

            mockMvc.perform(post("/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.emailUser").exists());
        }

        @Test
        @DisplayName("Should return 400 when password has no letters")
        void shouldReturn400_whenPasswordHasNoLetters() throws Exception {
            validRequest.setPasswordUser("123456");

            mockMvc.perform(post("/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.passwordUser").exists());
        }

        @Test
        @DisplayName("Should return 400 when role is invalid")
        void shouldReturn400_whenRoleIsInvalid() throws Exception {
            validRequest.setRoleUser("CUSTOMER");

            mockMvc.perform(post("/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.roleUser").exists());
        }

        @Test
        @DisplayName("Should return 409 when email already exists")
        void shouldReturn409_whenEmailAlreadyExists() throws Exception {
            when(requestMapper.toUser(any())).thenReturn(mockUser);
            when(userService.saveUser(any()))
                    .thenThrow(new IllegalArgumentException("A user with this email already exists"));

            mockMvc.perform(post("/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("A user with this email already exists"));
        }
    }

    @Nested
    @DisplayName("GET /user")
    class GetAllUsers {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 with user list when authenticated")
        void shouldReturn200_withUserList_whenAuthenticated() throws Exception {
            when(userService.getAllUsers()).thenReturn(List.of(mockUser));
            when(responseMapper.toUserResponseList(any())).thenReturn(List.of(mockUserResponse));

            mockMvc.perform(get("/user"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].nameUser").value("Ana Silva"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 200 with empty list when no users exist")
        void shouldReturn200_withEmptyList_whenNoUsersExist() throws Exception {
            when(userService.getAllUsers()).thenReturn(List.of());
            when(responseMapper.toUserResponseList(any())).thenReturn(List.of());

            mockMvc.perform(get("/user"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void shouldReturn401_whenUnauthenticated() throws Exception {
            mockMvc.perform(get("/user"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /user/{id}")
    class GetUserById {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 when user is found")
        void shouldReturn200_whenUserIsFound() throws Exception {
            when(userService.getUserById(1L)).thenReturn(mockUser);
            when(responseMapper.toUserResponse(any())).thenReturn(mockUserResponse);

            mockMvc.perform(get("/user/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.idUser").value(1L))
                    .andExpect(jsonPath("$.nameUser").value("Ana Silva"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when user is not found")
        void shouldReturn404_whenUserIsNotFound() throws Exception {
            when(userService.getUserById(99L))
                    .thenThrow(new ResourceNotFoundException("User not found with id: 99"));

            mockMvc.perform(get("/user/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User not found with id: 99"));
        }
    }

    @Nested
    @DisplayName("PUT /user/{id}")
    class UpdateUser {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 when user is updated")
        void shouldReturn200_whenUserIsUpdated() throws Exception {
            UserResponse updatedResponse = new UserResponse(
                    1L, "Ana Silva Updated", "a***a@techcellshop.com",
                    "+55 11 90000-0001", "Rua das Flores, 123 - Sao Paulo - SP", "USER"
            );
            when(userService.updateUser(eq(1L), any())).thenReturn(mockUser);
            when(responseMapper.toUserResponse(any())).thenReturn(updatedResponse);

            mockMvc.perform(put("/user/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(mockUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nameUser").value("Ana Silva Updated"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when user is not found")
        void shouldReturn404_whenUserIsNotFound() throws Exception {
            when(userService.updateUser(eq(99L), any()))
                    .thenThrow(new ResourceNotFoundException("User not found with id: 99"));

            mockMvc.perform(put("/user/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(mockUser)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User not found with id: 99"));
        }
    }

    @Nested
    @DisplayName("DELETE /user/{id}")
    class DeleteUser {

        @Test
        @WithMockUser
        @DisplayName("Should return 204 when user is deleted")
        void shouldReturn204_whenUserIsDeleted() throws Exception {
            doNothing().when(userService).deleteUser(1L);

            mockMvc.perform(delete("/user/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when user is not found")
        void shouldReturn404_whenUserIsNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("User not found with id: 99"))
                    .when(userService).deleteUser(99L);

            mockMvc.perform(delete("/user/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User not found with id: 99"));
        }
    }
}
