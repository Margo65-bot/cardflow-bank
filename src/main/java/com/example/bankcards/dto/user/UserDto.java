package com.example.bankcards.dto.user;

import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private Role role;

    public static UserDto fromEntity(User user) {
        if (user == null) return null;
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
