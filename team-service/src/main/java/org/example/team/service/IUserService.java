package org.example.team.service;

import org.example.team.dto.UserDto;

import java.util.List;

public interface IUserService {
    List<UserDto> getAllUsers();
    
    UserDto getUserById(Long id);
    
    List<UserDto> getUsersByTeamId(Long teamId);
    
    UserDto getUserByEmail(String email);
    
    UserDto createUser(UserDto userDto);
    
    UserDto updateUser(Long id, UserDto userDto);
    
    void deleteUser(Long id);
}
