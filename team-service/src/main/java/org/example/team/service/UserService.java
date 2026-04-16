package org.example.team.service;

import lombok.RequiredArgsConstructor;
import org.example.team.dto.UserDto;
import org.example.team.entity.Team;
import org.example.team.entity.User;
import org.example.team.repository.TeamRepository;
import org.example.team.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return convertToDto(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getUsersByTeamId(Long teamId) {
        return userRepository.findByTeamId(teamId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        UserDto dto = convertToDto(user);
        dto.setPassword(user.getPassword()); // Include password for authentication
        return dto;
    }

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
            Team team = teamRepository.findById(userDto.getTeamId())
                    .orElseThrow(() -> new RuntimeException("Team not found with id: " + userDto.getTeamId()));
            user.setTeam(team);
        }

        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }

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
            Team team = teamRepository.findById(userDto.getTeamId())
                    .orElseThrow(() -> new RuntimeException("Team not found with id: " + userDto.getTeamId()));
            user.setTeam(team);
        } else {
            user.setTeam(null);
        }

        User updatedUser = userRepository.save(user);
        return convertToDto(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setRole(user.getRole());
        if (user.getTeam() != null) {
            dto.setTeamId(user.getTeam().getId());
            dto.setTeamName(user.getTeam().getName());
        }
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}
