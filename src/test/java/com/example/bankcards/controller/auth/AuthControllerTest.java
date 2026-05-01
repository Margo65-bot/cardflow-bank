package com.example.bankcards.controller.auth;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.dto.security.AuthRequest;
import com.example.bankcards.dto.security.AuthResponse;
import com.example.bankcards.dto.security.RegisterRequest;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.exception.AlreadyExistsException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.security.JwtRequestFilter;
import com.example.bankcards.service.auth.AuthService;
import com.example.bankcards.service.user.UserService;
import com.example.bankcards.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.example.bankcards.entity.enums.Role.USER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtUtil.class, JwtRequestFilter.class, CustomUserDetailsService.class})
public class AuthControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    private final String validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0In0.signature";

    // ==================== LOGIN TESTS ====================

    @Test
    void login_shouldReturnToken_whenCredentialsAreValid() throws Exception {
        AuthRequest request = new AuthRequest("testuser", "password123");
        AuthResponse response = new AuthResponse(validToken);

        when(authService.login(any(AuthRequest.class))).thenReturn(response);

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(validToken));

        verify(authService, times(1)).login(any(AuthRequest.class));
    }

    @Test
    void login_shouldReturnUnauthorized_whenCredentialsAreInvalid() throws Exception {
        AuthRequest request = new AuthRequest("wronguser", "wrongpass");

        when(authService.login(any(AuthRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(authService, times(1)).login(any(AuthRequest.class));
    }

    // ==================== REGISTER TESTS ====================

    @Test
    void register_shouldReturnCreated_whenDataIsValid() throws Exception {
        RegisterRequest request = new RegisterRequest("newuser", "password123", "newuser@example.com");

        UserDto createdUser = UserDto.builder()
                .id(1L)
                .username("newuser")
                .email("newuser@example.com")
                .role(USER)
                .build();
        when(userService.register(any(RegisterRequest.class))).thenReturn(createdUser);  // ← thenReturn

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().string("User registered successfully"));

        verify(userService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    void register_shouldReturnConflict_whenUsernameAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest("existinguser", "password123", "new@example.com");

        doThrow(new AlreadyExistsException("Username already exists"))
                .when(userService).register(any(RegisterRequest.class));

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        verify(userService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    void register_shouldReturnConflict_whenEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest("newuser", "password123", "existing@example.com");

        doThrow(new AlreadyExistsException("Email already exists"))
                .when(userService).register(any(RegisterRequest.class));

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        verify(userService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    void register_shouldReturnBadRequest_whenUsernameIsBlank() throws Exception {
        RegisterRequest request = new RegisterRequest("", "password123", "test@example.com");

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).register(any());
    }

    @Test
    void register_shouldReturnBadRequest_whenPasswordIsBlank() throws Exception {
        RegisterRequest request = new RegisterRequest("newuser", "", "test@example.com");

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).register(any());
    }

    @Test
    void register_shouldReturnBadRequest_whenEmailIsInvalid() throws Exception {
        RegisterRequest request = new RegisterRequest("newuser", "password123", "not-an-email");

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).register(any());
    }

    @Test
    void login_shouldReturnBadRequest_whenUsernameIsBlank() throws Exception {
        AuthRequest request = new AuthRequest("", "password123");

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any());
    }

    @Test
    void login_shouldReturnBadRequest_whenPasswordIsBlank() throws Exception {
        AuthRequest request = new AuthRequest("testuser", "");

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any());
    }
}