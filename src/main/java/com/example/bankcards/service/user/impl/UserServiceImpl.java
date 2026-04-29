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
import com.example.bankcards.service.user.UserAdminService;
import com.example.bankcards.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService, UserAdminService {
    private final UserRepository userRepository;

    private final CardRepository cardRepository;

    // ========== ADMIN methods (UserAdminService) ==========
    @Override
    @Transactional
    public UserDto createUserByAdmin(CreateUserByAdminRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new AlreadyExistsException("Пользователь с данным именем уже существует");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new AlreadyExistsException("Пользователь с данным email уже существует");
        }

        User newUser = new User();
        newUser.setUsername(request.username());
        newUser.setPassword(request.password());
        newUser.setEmail(request.email());
        newUser.setRole(request.role());

        return UserDto.fromEntity(userRepository.save(newUser));
    }

    @Override
    public Page<UserDto> findAll(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserDto::fromEntity);
    }

    @Override
    @Transactional
    public UserDto changeRole(Long userId, Role role) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("Пользователь с id " + userId + " не найден")
        );

        if (role == Role.ADMIN && cardRepository.existsByUserId(userId)) {
            throw new AccessDeniedException("Нельзя назначить администратором пользователя, у которого есть карты");
        }

        user.setRole(role);
        return UserDto.fromEntity(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
        userRepository.deleteById(userId);
    }

    // ========== PUBLIC methods ==========

    @Override
    @Transactional
    public UserDto register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new AlreadyExistsException("Пользователь с данным именем уже существует");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new AlreadyExistsException("Пользователь с данным email уже существует");
        }

        User newUser = new User();
        newUser.setUsername(request.username());
        newUser.setPassword(request.password());
        newUser.setEmail(request.email());
        newUser.setRole(Role.USER);

        return UserDto.fromEntity(userRepository.save(newUser));
    }

    // ========== AUTHORIZED methods ==========

    @Override
    public UserDto findById(Long id) {
        return UserDto.fromEntity(
                userRepository.findById(id).orElseThrow(
                        () -> new NotFoundException("Пользователь с id " + id + " не найден")
                ));
    }

    // ========== INTERNAL methods ==========

    @Override
    public UserDto findByUsername(String username) {
        return UserDto.fromEntity(
                userRepository.findByUsername(username).orElseThrow(
                        () -> new NotFoundException("Пользователь с username " + username + " не найден")
                )
        );
    }
}
