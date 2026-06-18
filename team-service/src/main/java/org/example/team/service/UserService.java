package org.example.team.service;

import lombok.RequiredArgsConstructor;
import org.example.team.dto.UserDto;
import org.example.team.entity.Team;
import org.example.team.entity.User;
import org.example.team.mapper.UserMapper;
import org.example.team.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {
    private final UserRepository userRepository;
    private final ITeamService teamService;

    /**
     * Получить список всех пользователей в системе
     * 
     * @return список всех пользователей (без паролей)
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Получить пользователя по ID
     * 
     * @param id - ID пользователя
     * @return пользователь (без пароля)
     * @throws RuntimeException если пользователь не найден
     */
    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return UserMapper.toDto(user);
    }

    /**
     * Получить всех пользователей команды
     * 
     * @param teamId - ID команды
     * @return список пользователей команды (без паролей)
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getUsersByTeamId(Long teamId) {
        return userRepository.findByTeamId(teamId).stream()
                .map(UserMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Получить пользователя по email (используется Auth Service для логина)
     * 
     * ВАЖНО: Возвращает пользователя С ПАРОЛЕМ для проверки при логине
     * 
     * @param email - email пользователя
     * @return пользователь с паролем (BCrypt hash)
     * @throws RuntimeException если пользователь не найден
     */
    @Override
    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        return UserMapper.toDtoWithPassword(user);
    }

    /**
     * Создать нового пользователя (вызывается из Auth Service при регистрации)
     * 
     * ПРОВЕРКИ:
     * - Email должен быть уникальным
     * - Пароль должен быть уже захеширован Auth Service (BCrypt)
     * 
     * @param userDto - данные нового пользователя (email, name, password, role, teamId)
     * @return созданный пользователь (без пароля)
     * @throws RuntimeException если email уже занят
     */
    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new RuntimeException("User with email '" + userDto.getEmail() + "' already exists");
        }

        User user = new User();
        user.setEmail(userDto.getEmail());
        user.setName(userDto.getName());
        user.setRole(userDto.getRole());
        
        // Set password (should be hashed by auth-service before calling this)
        if (userDto.getPassword() != null) {
            user.setPassword(userDto.getPassword());
        } else {
            // Default password if not provided (for backward compatibility)
            user.setPassword("changeme");
        }

        if (userDto.getTeamId() != null) {
            Team team = teamService.getTeamEntityById(userDto.getTeamId());
            user.setTeam(team);
        }

        User savedUser = userRepository.save(user);
        return UserMapper.toDto(savedUser);
    }

    /**
     * Обновить данные пользователя
     * 
     * ПРОВЕРКИ:
     * - Если меняется email, он должен быть уникальным
     * - Можно изменить: email, name, role, teamId
     * - Пароль НЕ обновляется этим методом (отдельный эндпоинт)
     * 
     * @param id - ID пользователя для обновления
     * @param userDto - новые данные пользователя
     * @return обновленный пользователь (без пароля)
     * @throws RuntimeException если пользователь не найден или email занят
     */
    @Override
    @Transactional
    public UserDto updateUser(Long id, UserDto userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        if (!user.getEmail().equals(userDto.getEmail()) && userRepository.existsByEmail(userDto.getEmail())) {
            throw new RuntimeException("User with email '" + userDto.getEmail() + "' already exists");
        }

        user.setEmail(userDto.getEmail());
        user.setName(userDto.getName());
        user.setRole(userDto.getRole());

        if (userDto.getTeamId() != null) {
            Team team = teamService.getTeamEntityById(userDto.getTeamId());
            user.setTeam(team);
        } else {
            user.setTeam(null);
        }

        User updatedUser = userRepository.save(user);
        return UserMapper.toDto(updatedUser);
    }

    /**
     * Удалить пользователя
     * 
     * ВНИМАНИЕ: Удаление пользователя может нарушить связи с задачами и спринтами
     * В production лучше использовать soft delete (флаг isDeleted)
     * 
     * @param id - ID пользователя для удаления
     * @throws RuntimeException если пользователь не найден
     */
    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}
