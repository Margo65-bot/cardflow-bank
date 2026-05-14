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

/**
 * Реализация сервисов для работы с пользователями.
 *
 * <p>Реализует оба интерфейса: {@link UserService} и {@link UserAdminService}.
 * Методы разделены на секции {@code ADMIN}, {@code PUBLIC} и {@code INTERNAL}
 * для наглядного разделения уровней доступа.</p>
 *
 * <p>Бизнес-правила:</p>
 * <ul>
 *   <li>Username и email должны быть уникальными</li>
 *   <li>Пароли хешируются через BCrypt (в сущности {@link User})</li>
 *   <li>Нельзя назначить роль ADMIN пользователю с картами (ADMIN не должен иметь карты)</li>
 *   <li>Публичная регистрация всегда создаёт USER</li>
 * </ul>
 *
 * @see User
 * @see Role
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService, UserAdminService {
    private final UserRepository userRepository;

    private final CardRepository cardRepository;

    // ========== ADMIN methods (UserAdminService) ==========

    /**
     * Создаёт пользователя с указанной ролью (админская функция).
     *
     * <p>В отличие от {@link #register}, позволяет явно указать роль.</p>
     *
     * @param request данные пользователя
     * @return созданный пользователь
     * @throws AlreadyExistsException если username или email заняты
     */
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

    /**
     * Возвращает всех пользователей с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return страница с пользователями
     */
    @Override
    public Page<UserDto> findAll(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserDto::fromEntity);
    }

    /**
     * Изменяет роль пользователя.
     *
     * <p>Особое правило: нельзя назначить {@code ADMIN} пользователю,
     * у которого есть карты. Это предотвращает конфликт интересов —
     * администратор управляет системой, не имея своих карт в ней.</p>
     *
     * @param userId идентификатор пользователя
     * @param role   новая роль
     * @return обновлённый пользователь
     * @throws NotFoundException если пользователь не найден
     * @throws AccessDeniedException если у пользователя есть карты и роль ADMIN
     */
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

    /**
     * Удаляет пользователя и все его данные.
     *
     * <p>Удаление каскадное: карты и транзакции удалятся автоматически
     * благодаря настройкам внешних ключей в БД.</p>
     *
     * @param userId идентификатор пользователя
     * @throws NotFoundException если пользователь не найден
     */
    @Override
    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
        userRepository.deleteById(userId);
    }

    // ========== PUBLIC methods ==========


    /**
     * Регистрирует нового пользователя с ролью {@code USER}.
     *
     * <p>Пароль автоматически хешируется через BCrypt (в сущности {@link User#setPassword}).</p>
     *
     * @param request данные для регистрации
     * @return созданный пользователь
     * @throws AlreadyExistsException если username или email заняты
     */
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

    /**
     * Возвращает данные пользователя по ID.
     *
     * <p>Пароль в ответе не передаётся.</p>
     *
     * @param id идентификатор пользователя
     * @return DTO пользователя
     * @throws NotFoundException если пользователь не найден
     */
    @Override
    public UserDto findById(Long id) {
        return UserDto.fromEntity(
                userRepository.findById(id).orElseThrow(
                        () -> new NotFoundException("Пользователь с id " + id + " не найден")
                ));
    }

    // ========== INTERNAL methods ==========

    /**
     * Находит пользователя по username.
     *
     * <p><b>Только для внутреннего использования.</b>
     * Применяется в {@link com.example.bankcards.service.auth.impl.AuthServiceImpl} при аутентификации.</p>
     *
     * @param username имя пользователя
     * @return DTO пользователя
     * @throws NotFoundException если пользователь не найден
     */
    @Override
    public UserDto findByUsername(String username) {
        return UserDto.fromEntity(
                userRepository.findByUsername(username).orElseThrow(
                        () -> new NotFoundException("Пользователь с username " + username + " не найден")
                )
        );
    }
}
