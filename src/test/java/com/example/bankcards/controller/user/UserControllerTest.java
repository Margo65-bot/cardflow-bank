package com.example.bankcards.controller.user;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.security.JwtRequestFilter;
import com.example.bankcards.service.user.UserService;
import com.example.bankcards.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtUtil.class, JwtRequestFilter.class, CustomUserDetailsService.class})
public class UserControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    private final Long userId = 1L;
    private final String username = "testuser";
    private final String email = "test@example.com";
    private final Role role = Role.USER;

    @BeforeEach
    void setUp() {
        CustomUserDetails userDetails = new CustomUserDetails(
                userId,
                username,
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    @Test
    void getMyProfile_shouldReturnUser_whenAuthenticated() throws Exception {
        UserDto userDto = UserDto.builder()
                .id(userId)
                .username(username)
                .email(email)
                .role(role)
                .build();

        when(userService.findById(userId)).thenReturn(userDto);

        mvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void getMyProfile_shouldReturnUnauthorized_whenNoAuth() throws Exception {
        SecurityContextHolder.clearContext();

        mvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden());
    }
}