package org.example.team.mapper;

import org.example.team.dto.UserDto;
import org.example.team.entity.User;

public final class UserMapper {
    
    private UserMapper() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static UserDto toDto(User user) {
        if (user == null) {
            return null;
        }
        
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
    
    public static UserDto toDtoWithPassword(User user) {
        if (user == null) {
            return null;
        }
        
        UserDto dto = toDto(user);
        dto.setPassword(user.getPassword());
        return dto;
    }
    
    public static User toEntity(UserDto dto) {
        if (dto == null) {
            return null;
        }
        
        User user = new User();
        user.setId(dto.getId());
        user.setEmail(dto.getEmail());
        user.setName(dto.getName());
        user.setRole(dto.getRole());
        user.setPassword(dto.getPassword());
        return user;
    }
}
