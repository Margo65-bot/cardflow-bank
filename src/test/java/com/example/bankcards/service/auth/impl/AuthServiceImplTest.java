package com.example.bankcards.service.auth.impl;

import com.example.bankcards.dto.security.AuthRequest;
import com.example.bankcards.dto.security.AuthResponse;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.service.user.UserService;
import com.example.bankcards.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthServiceImpl authService;

    private final String username = "testuser";
    private final String password = "password123";
    private final Long userId = 1L;
    private final Role role = Role.USER;
    private final String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0In0.signature";

    @Test
    void login_ShouldReturnAuthResponse_WhenCredentialsAreValid() {
        AuthRequest request = new AuthRequest(username, password);
        UserDto userDto = UserDto.builder()
                .id(userId)
                .username(username)
                .email("test@example.com")
                .role(role)
                .build();

        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userService.findByUsername(username)).thenReturn(userDto);
        when(jwtUtil.generateToken(userId, username, role.name())).thenReturn(token);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo(token);

        verify(authenticationManager, times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userService, times(1)).findByUsername(username);
        verify(jwtUtil, times(1)).generateToken(userId, username, role.name());

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isEqualTo(authentication);
    }

    @Test
    void login_ShouldThrowBadCredentialsException_WhenPasswordIsInvalid() {
        AuthRequest request = new AuthRequest(username, "wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Bad credentials");

        verify(authenticationManager, times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userService, never()).findByUsername(any());
        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }

    @Test
    void login_ShouldThrowException_WhenUserNotFound() {
        AuthRequest request = new AuthRequest(username, password);
        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userService.findByUsername(username))
                .thenThrow(new RuntimeException("User not found"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");

        verify(authenticationManager, times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userService, times(1)).findByUsername(username);
        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }

    @Test
    void login_ShouldSetAuthenticationInSecurityContext() {
        AuthRequest request = new AuthRequest(username, password);
        UserDto userDto = UserDto.builder()
                .id(userId)
                .username(username)
                .email("test@example.com")
                .role(role)
                .build();

        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userService.findByUsername(username)).thenReturn(userDto);
        when(jwtUtil.generateToken(userId, username, role.name())).thenReturn(token);

        SecurityContextHolder.clearContext();

        authService.login(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isEqualTo(authentication);

        SecurityContextHolder.clearContext();
    }

    @Test
    void login_ShouldUseCorrectUsernameAndPasswordInAuthentication() {
        AuthRequest request = new AuthRequest(username, password);
        UserDto userDto = UserDto.builder()
                .id(userId)
                .username(username)
                .email("test@example.com")
                .role(role)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenAnswer(invocation -> {
                    UsernamePasswordAuthenticationToken authToken = invocation.getArgument(0);
                    assertThat(authToken.getPrincipal()).isEqualTo(username);
                    assertThat(authToken.getCredentials()).isEqualTo(password);
                    return mock(Authentication.class);
                });
        when(userService.findByUsername(username)).thenReturn(userDto);
        when(jwtUtil.generateToken(userId, username, role.name())).thenReturn(token);

        authService.login(request);

        verify(authenticationManager, times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}