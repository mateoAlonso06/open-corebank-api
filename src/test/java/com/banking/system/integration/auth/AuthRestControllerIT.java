package com.banking.system.integration.auth;

import com.banking.system.auth.domain.model.enums.UserStatus;
import com.banking.system.auth.infraestructure.adapter.out.persistence.entity.VerificationTokenJpaEntity;
import com.banking.system.auth.infraestructure.adapter.out.persistence.repository.SpringDataUserRepository;
import com.banking.system.auth.infraestructure.adapter.out.persistence.repository.SpringVerificationTokenJpaRepository;
import com.banking.system.customer.infraestructure.adapter.out.persistence.repository.SpringDataCustomerRepository;
import com.banking.system.integration.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthRestControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringVerificationTokenJpaRepository tokenRepository;

    @Autowired
    private SpringDataCustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        // Clean database before each test to ensure test isolation
        tokenRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        Map<String, Object> registerRequest = createRegisterRequest();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.email").value(registerRequest.get("email")));
    }

    @Test
    void shouldFailRegisterWithInvalidEmail() throws Exception {
        Map<String, Object> registerRequest = createRegisterRequest();

        registerRequest.put("email", "invalid-email-format");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFailRegisterWithShortPassword() throws Exception {
        Map<String, Object> registerRequest = new HashMap<>();

        registerRequest.put("password", "short");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFailRegisterWithMissingRequiredFields() throws Exception {
        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("email", "test3@example.com");
        registerRequest.put("password", "SecurePass123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFailRegisterWithDuplicateEmail() throws Exception {
        Map<String, Object> registerRequest = createRegisterRequest();

        // First registration
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.email").value(registerRequest.get("email")));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldFailRegisterWithDuplicateDni() throws Exception {
        Map<String, Object> registerRequest = createRegisterRequest();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.email").value(registerRequest.get("email")));

        registerRequest.put("email", "anothermailtest@gmail.com");
        // same dni number

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldFailRegisterWithInvalidPhoneNumber() throws Exception {
        Map<String, Object> registerRequest = createRegisterRequest();

        registerRequest.put("phone", "invalid-phone-number");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFailRegisterWithUnderageBirthDate() throws Exception {
        Map<String, Object> registerRequest = createRegisterRequest();

        registerRequest.put("birthDate", LocalDate.of(2010, 1, 1).toString());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        var registerRequest = createRegisterRequest();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        var token = getEmailVerificationToken(registerRequest).getToken();

        var verifyEmailRequest = new HashMap<>();
        verifyEmailRequest.put("token", token);

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyEmailRequest)))
                .andExpect(status().isOk());

        var loginRequest = createLoginRequest();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.token").value(notNullValue()));
    }

    @Test
    void shouldFailLoginWithInvalidCredentials() throws Exception {
        // First, register a user
        var registerRequest = createRegisterRequest();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        var loginRequest = createLoginRequest();

        loginRequest.put("password", "WrongPassword123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldFailLoginWhenUserIsBlocked() throws Exception {
        var registerRequest = createRegisterRequest();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        var token = getEmailVerificationToken(registerRequest).getToken();
        var verifyEmailRequest = new HashMap<>();
        verifyEmailRequest.put("token", token);

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyEmailRequest)))
                .andExpect(status().isOk());

        var user = userRepository.findByEmail((String) registerRequest.get("email"))
                .orElseThrow(() -> new RuntimeException("User not found with email: " + registerRequest.get("email")));
        user.setStatus(UserStatus.BLOCKED);
        userRepository.save(user);

        var loginRequest = createLoginRequest();
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isLocked());
    }

    @Test
    void shouldFailLoginWithNonExistentUser() throws Exception {
        var loginRequest = createLoginRequest();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldFailLoginWithInvalidEmailFormat() throws Exception {
        var loginRequest = createLoginRequest();

        loginRequest.put("email", "invalid-email-format");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFailLoginWithShortPassword() throws Exception {
        var loginRequest = createLoginRequest();

        loginRequest.put("password", "short");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldFailLoginWhenEmailIsNotVerified() throws Exception {
        var registerRequest = createRegisterRequest();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        var loginRequest = createLoginRequest();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldVerifiedEmailSuccessfully() throws Exception {
        var registerRequest = createRegisterRequest();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        var token = getEmailVerificationToken(registerRequest).getToken();

        var verifyEmailRequest = new HashMap<>();
        verifyEmailRequest.put("token", token);

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyEmailRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldFailEmailVerificationWhenTokenIsInvalid() throws Exception {
        var registerRequest = createRegisterRequest();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        var token = "invalid-token";

        var verifyEmailRequest = new HashMap<>();
        verifyEmailRequest.put("token", token);

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyEmailRequest)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldFailVerificationEmailWhenTokenIsExpired() throws Exception {
        var registerRequest = createRegisterRequest();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        var expiredToken = getEmailVerificationToken(registerRequest);
        expiredToken.setExpiresAt(LocalDateTime.now().minusMinutes(16));
        tokenRepository.save(expiredToken);

        var verifyEmailRequest = new HashMap<>();
        verifyEmailRequest.put("token", expiredToken.getToken());

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyEmailRequest)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldFailVerificationWhenUserIsAlreadyAuthenticated() throws Exception {
        var registerRequest = createRegisterRequest();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        var token = getEmailVerificationToken(registerRequest).getToken();
        var verifyEmailRequest = new HashMap<>();
        verifyEmailRequest.put("token", token);

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyEmailRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyEmailRequest)))
                .andExpect(status().isUnprocessableEntity());
    }

    private Map<String, Object> createRegisterRequest() {
        Map<String, Object> request = new HashMap<>();

        request.put("email", "test@example.com");
        request.put("password", "Secure_Pass123");
        request.put("firstName", "John");
        request.put("lastName", "Doe");
        request.put("documentType", "DNI");
        request.put("documentNumber", "12345678");
        request.put("birthDate", LocalDate.of(1990, 1, 1).toString());
        request.put("phone", "+5491112345678"); // Valid Argentinian phone number format
        request.put("city", "Buenos Aires");
        request.put("country", "AR");
        request.put("address", "Av. Corrientes 1234");

        return request;
    }

    private Map<String, String> createLoginRequest() {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "test@example.com");
        loginRequest.put("password", "Secure_Pass123");
        return loginRequest;
    }

    private VerificationTokenJpaEntity getEmailVerificationToken(Map<String, Object> registerRequest) {
        String email = (String) registerRequest.get("email");

        UUID userId = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email))
                .getId();

        return tokenRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new RuntimeException("Verification token not found for user ID: " + userId));
    }
}
