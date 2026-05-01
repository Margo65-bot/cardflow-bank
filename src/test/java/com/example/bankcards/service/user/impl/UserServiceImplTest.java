package com.example.bankcards.service.user.impl;

import com.example.bankcards.dto.security.RegisterRequest;
import com.example.bankcards.dto.user.CreateUserByAdminRequest;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.exception.AccessDeniedException;
import com.example.bankcards.exception.AlreadyExistsException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserDto testUserDto;

    private final Long userId = 1L;
    private final String username = "testuser";
    private final String email = "test@example.com";
    private final String password = "password123";
    private final Role role = Role.USER;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername(username);
        testUser.setEmail(email);
        testUser.setRole(role);
        testUser.setPassword("encodedPassword");

        testUserDto = UserDto.builder()
                .id(userId)
                .username(username)
                .email(email)
                .role(role)
                .build();
    }

    // ==================== REGISTER (PUBLIC) ====================

    @Test
    void register_ShouldCreateUser_WhenDataIsValid() {
        RegisterRequest request = new RegisterRequest(username, password, email);

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = userService.register(request);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getRole()).isEqualTo(Role.USER);

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_ShouldThrowAlreadyExistsException_WhenUsernameExists() {
        RegisterRequest request = new RegisterRequest(username, password, email);

        when(userRepository.existsByUsername(username)).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("Пользователь с данным именем уже существует");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_ShouldThrowAlreadyExistsException_WhenEmailExists() {
        RegisterRequest request = new RegisterRequest(username, password, email);

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("Пользователь с данным email уже существует");

        verify(userRepository, never()).save(any());
    }

    // ==================== CREATE USER BY ADMIN ====================

    @Test
    void createUserByAdmin_ShouldCreateUser_WhenDataIsValid() {
        CreateUserByAdminRequest request = new CreateUserByAdminRequest(
                username, password, email, Role.ADMIN
        );

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = userService.createUserByAdmin(request);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void createUserByAdmin_ShouldThrowAlreadyExistsException_WhenUsernameExists() {
        CreateUserByAdminRequest request = new CreateUserByAdminRequest(
                username, password, email, Role.ADMIN
        );

        when(userRepository.existsByUsername(username)).thenReturn(true);

        assertThatThrownBy(() -> userService.createUserByAdmin(request))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("Пользователь с данным именем уже существует");
    }

    @Test
    void createUserByAdmin_ShouldThrowAlreadyExistsException_WhenEmailExists() {
        CreateUserByAdminRequest request = new CreateUserByAdminRequest(
                username, password, email, Role.ADMIN
        );

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(true);

        assertThatThrownBy(() -> userService.createUserByAdmin(request))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("Пользователь с данным email уже существует");
    }

    // ==================== FIND ALL (ADMIN) ====================

    @Test
    void findAll_ShouldReturnPageOfUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(testUser), pageable, 1);

        when(userRepository.findAll(pageable)).thenReturn(userPage);

        Page<UserDto> result = userService.findAll(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo(username);
        verify(userRepository, times(1)).findAll(pageable);
    }

    // ==================== CHANGE ROLE (ADMIN) ====================

    @Test
    void changeRole_ShouldChangeRole_WhenUserExistsAndNoCards() {
        Role newRole = Role.ADMIN;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(cardRepository.existsByUserId(userId)).thenReturn(false);

        UserDto result = userService.changeRole(userId, newRole);

        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo(newRole);
        assertThat(testUser.getRole()).isEqualTo(newRole);

        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void changeRole_ShouldThrowNotFoundException_WhenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changeRole(999L, Role.ADMIN))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    void changeRole_ShouldThrowAccessDeniedException_WhenUserHasCards() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(cardRepository.existsByUserId(userId)).thenReturn(true);

        assertThatThrownBy(() -> userService.changeRole(userId, Role.ADMIN))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Нельзя назначить администратором пользователя, у которого есть карты");
    }

    // ==================== DELETE USER (ADMIN) ====================

    @Test
    void deleteUser_ShouldDeleteUser_WhenUserExists() {
        when(userRepository.existsById(userId)).thenReturn(true);
        doNothing().when(userRepository).deleteById(userId);

        userService.deleteUser(userId);

        verify(userRepository, times(1)).deleteById(userId);
    }

    @Test
    void deleteUser_ShouldThrowNotFoundException_WhenUserNotFound() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("не найден");
    }

    // ==================== FIND BY ID ====================

    @Test
    void findById_ShouldReturnUserDto_WhenUserExists() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        UserDto result = userService.findById(userId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getUsername()).isEqualTo(username);
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void findById_ShouldThrowNotFoundException_WhenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("не найден");
    }

    // ==================== FIND BY USERNAME ====================

    @Test
    void findByUsername_ShouldReturnUserDto_WhenUserExists() {
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        UserDto result = userService.findByUsername(username);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    void findByUsername_ShouldThrowNotFoundException_WhenUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByUsername("unknown"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("не найден");
    }
}