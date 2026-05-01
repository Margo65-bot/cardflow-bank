package com.example.bankcards.controller.user;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.dto.user.CreateUserByAdminRequest;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.exception.AlreadyExistsException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.security.JwtRequestFilter;
import com.example.bankcards.service.user.UserAdminService;
import com.example.bankcards.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
@Import({SecurityConfig.class, JwtUtil.class, JwtRequestFilter.class, CustomUserDetailsService.class})
public class AdminUserControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private UserAdminService userService;

    @MockitoBean
    private UserRepository userRepository;

    private final Long adminUserId = 1L;
    private final String adminUsername = "adminuser";
    private final String adminEmail = "admin@example.com";
    private final Role adminRole = Role.ADMIN;

    @BeforeEach
    void setUp() {
        CustomUserDetails adminDetails = new CustomUserDetails(
                adminUserId,
                adminUsername,
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_" + adminRole.name()))
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(adminDetails, null, adminDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    // ==================== GET ALL USERS ====================

    @Test
    void getAllUsers_shouldReturnPageOfUsers() throws Exception {
        UserDto userDto = UserDto.builder()
                .id(adminUserId)
                .username(adminUsername)
                .email(adminEmail)
                .role(adminRole)
                .build();

        Page<UserDto> page = new PageImpl<>(List.of(userDto), PageRequest.of(0, 10), 1);

        when(userService.findAll(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/api/admin/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(adminUserId))
                .andExpect(jsonPath("$.content[0].username").value(adminUsername));
    }

    @Test
    void getAllUsers_shouldReturnForbidden_whenNoAuth() throws Exception {
        SecurityContextHolder.clearContext();

        mvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    // ==================== CREATE USER ====================

    @Test
    void createUser_shouldReturnCreated_whenValid() throws Exception {
        CreateUserByAdminRequest request = new CreateUserByAdminRequest(
                "newuser", "password123", "new@example.com", Role.USER
        );

        UserDto createdUser = UserDto.builder()
                .id(2L)
                .username("newuser")
                .email("new@example.com")
                .role(Role.USER)
                .build();

        when(userService.createUserByAdmin(any(CreateUserByAdminRequest.class))).thenReturn(createdUser);

        mvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/admin/users/2"))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    void createUser_shouldReturnBadRequest_whenUsernameIsBlank() throws Exception {
        CreateUserByAdminRequest request = new CreateUserByAdminRequest(
                "", "password123", "new@example.com", Role.USER
        );

        mvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUserByAdmin(any());
    }

    @Test
    void createUser_shouldReturnBadRequest_whenEmailIsInvalid() throws Exception {
        CreateUserByAdminRequest request = new CreateUserByAdminRequest(
                "newuser", "password123", "not-an-email", Role.USER
        );

        mvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUserByAdmin(any());
    }

    @Test
    void createUser_shouldReturnConflict_whenUsernameExists() throws Exception {
        CreateUserByAdminRequest request = new CreateUserByAdminRequest(
                "existinguser", "password123", "new@example.com", Role.USER
        );

        when(userService.createUserByAdmin(any(CreateUserByAdminRequest.class)))
                .thenThrow(new AlreadyExistsException("Username already exists"));

        mvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        verify(userService, times(1)).createUserByAdmin(any());
    }

    // ==================== CHANGE USER ROLE ====================

    @Test
    void changeUserRole_shouldReturnUpdatedUser() throws Exception {
        UserDto updatedUser = UserDto.builder()
                .id(adminUserId)
                .username(adminUsername)
                .email(adminEmail)
                .role(Role.ADMIN)
                .build();

        when(userService.changeRole(eq(adminUserId), eq(Role.ADMIN))).thenReturn(updatedUser);

        mvc.perform(put("/api/admin/users/{id}/role", adminUserId)
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void changeUserRole_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        when(userService.changeRole(eq(999L), any(Role.class)))
                .thenThrow(new NotFoundException("User not found"));

        mvc.perform(put("/api/admin/users/{id}/role", 999L)
                        .param("role", "ADMIN"))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE USER ====================

    @Test
    void deleteUser_shouldReturnNoContent_whenSuccess() throws Exception {
        doNothing().when(userService).deleteUser(adminUserId);

        mvc.perform(delete("/api/admin/users/{id}", adminUserId))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(adminUserId);
    }

    @Test
    void deleteUser_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        doThrow(new NotFoundException("User not found"))
                .when(userService).deleteUser(999L);

        mvc.perform(delete("/api/admin/users/{id}", 999L))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).deleteUser(999L);
    }
}