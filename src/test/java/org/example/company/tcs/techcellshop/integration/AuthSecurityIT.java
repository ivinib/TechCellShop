package org.example.company.tcs.techcellshop.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.dto.request.AuthRequest;
import org.example.company.tcs.techcellshop.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@DisplayName("Auth and Security Integration Tests")
class AuthSecurityIT extends AbstractPostgresIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Should login successfully and return JWT token")
    void login_shouldReturnJwtToken() throws Exception {
        String email = uniqueEmail("auth_admin");
        createUser(email, "password123", "ADMIN");

        AuthRequest request = new AuthRequest(email, "password123");

        String responseBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(responseBody);
        String token = json.get("token").asText();

        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Should return 401 when protected endpoint is accessed without token")
    void protectedEndpoint_shouldReturn401_whenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/orders/me")
                        .header("X-Trace-Id", "trace-auth-001"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.traceId").value("trace-auth-001"));
    }

    @Test
    @DisplayName("Should return 401 when protected endpoint is accessed with malformed token")
    void protectedEndpoint_shouldReturn401_whenTokenIsMalformed() throws Exception {
        mockMvc.perform(get("/api/v1/orders/me")
                        .header("Authorization", "Bearer not-a-valid-jwt")
                        .header("X-Trace-Id", "trace-auth-002"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.traceId").value("trace-auth-002"));
    }

    @Test
    @DisplayName("Should allow admin token to access admin endpoint")
    void adminToken_shouldAccessAdminEndpoint() throws Exception {
        String email = uniqueEmail("admin_access");
        createUser(email, "password123", "ADMIN");
        String token = loginAndGetToken(email, "password123");

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists());
    }

    @Test
    @DisplayName("Should return 403 when user token accesses admin endpoint")
    void userToken_shouldReturn403_whenAccessingAdminEndpoint() throws Exception {
        String email = uniqueEmail("user_forbidden");
        createUser(email, "password123", "USER");
        String token = loginAndGetToken(email, "password123");

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Trace-Id", "trace-auth-003"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.traceId").value("trace-auth-003"));
    }

    @Test
    @DisplayName("Should allow authenticated user token to access own orders endpoint")
    void userToken_shouldAccessProtectedUserEndpoint() throws Exception {
        String email = uniqueEmail("user_orders");
        createUser(email, "password123", "USER");
        String token = loginAndGetToken(email, "password123");

        mockMvc.perform(get("/api/v1/orders/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.totalElements").exists());
    }

    private String loginAndGetToken(String email, String rawPassword) throws Exception {
        AuthRequest request = new AuthRequest(email, rawPassword);

        String responseBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(responseBody);
        return json.get("token").asText();
    }

    private User createUser(String email, String rawPassword, String role) {
        User user = new User();
        user.setNameUser("Security Test User");
        user.setEmailUser(email);
        user.setPasswordUser(passwordEncoder.encode(rawPassword));
        user.setPhoneUser("+55 11 90000-5555");
        user.setAddressUser("Security Test City");
        user.setRoleUser(role);
        return userRepository.save(user);
    }

    private String uniqueEmail(String prefix) {
        return prefix + "_" + System.nanoTime() + "@techcellshop.com";
    }
}