package com.example.bankcards.service.user;

import com.example.bankcards.dto.user.CreateUserByAdminRequest;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserAdminService {
    UserDto createUserByAdmin(CreateUserByAdminRequest request);

    Page<UserDto> findAll(Pageable pageable);

    UserDto changeRole(Long userId, Role role);

    void deleteUser(Long userId);
}
