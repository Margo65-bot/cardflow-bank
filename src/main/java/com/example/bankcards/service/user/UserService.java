package com.example.bankcards.service.user;

import com.example.bankcards.dto.security.RegisterRequest;
import com.example.bankcards.dto.user.UserDto;

public interface UserService {
    UserDto register(RegisterRequest request);

    UserDto findById(Long userId);

    UserDto findByUsername(String username);
}