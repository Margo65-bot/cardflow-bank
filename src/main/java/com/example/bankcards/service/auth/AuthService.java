package com.example.bankcards.service.auth;

import com.example.bankcards.dto.security.AuthRequest;
import com.example.bankcards.dto.security.AuthResponse;

public interface AuthService {
    AuthResponse login(AuthRequest request);
}
