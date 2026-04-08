package org.example.company.tcs.techcellshop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.company.tcs.techcellshop.config.SecurityConfig;
import org.example.company.tcs.techcellshop.dto.request.UserEnrollmentRequest;
import org.example.company.tcs.techcellshop.dto.request.UserUpdateRequest;
import org.example.company.tcs.techcellshop.dto.response.UserResponse;
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
import org.springframework.web.context.WebApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

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
    private UserUpdateRequest validUpdateRequest;
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

        validUpdateRequest = new UserUpdateRequest();
        validUpdateRequest.setNameUser("Ana Silva");
        validUpdateRequest.setEmailUser("ana@techcellshop.com");
        validUpdateRequest.setPhoneUser("+55 11 90000-0001");
        validUpdateRequest.setAddressUser("Rua das Flores, 123 - Sao Paulo - SP");
        validUpdateRequest.setRoleUser("USER");

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
    @DisplayName("POST /api/v1/users")
    class SaveUser {

        @Test
        @DisplayName("Should return 201 when request is valid")
        void shouldReturn201_whenRequestIsValid() throws Exception {
            when(requestMapper.toUser(any())).thenReturn(mockUser);
            when(userService.saveUser(any())).thenReturn(mockUser);
            when(responseMapper.toUserResponse(any())).thenReturn(mockUserResponse);

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/api/v1/users/1")))
                    .andExpect(jsonPath("$.idUser").value(1L))
                    .andExpect(jsonPath("$.nameUser").value("Ana Silva"));
        }

        @Test
        @DisplayName("Should return 400 when email already exists")
        void shouldReturn400_whenEmailAlreadyExists() throws Exception {
            when(requestMapper.toUser(any())).thenReturn(mockUser);
            when(userService.saveUser(any()))
                    .thenThrow(new IllegalArgumentException("A user with this email already exists"));

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value("A user with this email already exists"))
                    .andExpect(jsonPath("$.path").value("/api/v1/users"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users")
    class GetAllUsers {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 200 with user list when authenticated")
        void shouldReturn200_withUserList_whenAuthenticated() throws Exception {
            Page<User> usersPage = new PageImpl<>(List.of(mockUser), PageRequest.of(0, 20), 1);

            when(userService.getAllUsers(any(Pageable.class))).thenReturn(usersPage);
            when(responseMapper.toUserResponse(mockUser)).thenReturn(mockUserResponse);

            mockMvc.perform(get("/api/v1/users")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].nameUser").value("Ana Silva"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 200 with empty list when no users exist")
        void shouldReturn200_withEmptyList_whenNoUsersExist() throws Exception {
            Page<User> usersPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

            when(userService.getAllUsers(any(Pageable.class))).thenReturn(usersPage);

            mockMvc.perform(get("/api/v1/users")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0));
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void shouldReturn401_whenUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{id}")
    class GetUserById {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 when user is found")
        void shouldReturn200_whenUserIsFound() throws Exception {
            when(userService.getUserById(1L)).thenReturn(mockUser);
            when(responseMapper.toUserResponse(any())).thenReturn(mockUserResponse);

            mockMvc.perform(get("/api/v1/users/1"))
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

            mockMvc.perform(get("/api/v1/users/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("User not found with id: 99"))
                    .andExpect(jsonPath("$.path").value("/api/v1/users/99"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/{id}")
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

            mockMvc.perform(put("/api/v1/users/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nameUser").value("Ana Silva Updated"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when user is not found")
        void shouldReturn404_whenUserIsNotFound() throws Exception {
            when(userService.updateUser(eq(99L), any()))
                    .thenThrow(new ResourceNotFoundException("User not found with id: 99"));

            mockMvc.perform(put("/api/v1/users/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("User not found with id: 99"))
                    .andExpect(jsonPath("$.path").value("/api/v1/users/99"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/users/{id}")
    class PartialUpdateUser {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 when user is partially updated")
        void shouldReturn200_whenUserIsPartiallyUpdated() throws Exception {
            when(userService.updateUser(eq(1L), any())).thenReturn(mockUser);
            when(responseMapper.toUserResponse(any())).thenReturn(mockUserResponse);

            mockMvc.perform(patch("/api/v1/users/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.idUser").value(1L));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/users/{id}")
    class DeleteUser {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 204 when user is deleted")
        void shouldReturn204_whenUserIsDeleted() throws Exception {
            doNothing().when(userService).deleteUser(1L);

            mockMvc.perform(delete("/api/v1/users/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 404 when user is not found")
        void shouldReturn404_whenUserIsNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("User not found with id: 99"))
                    .when(userService).deleteUser(99L);

            mockMvc.perform(delete("/api/v1/users/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("User not found with id: 99"))
                    .andExpect(jsonPath("$.path").value("/api/v1/users/99"));
        }
    }
}