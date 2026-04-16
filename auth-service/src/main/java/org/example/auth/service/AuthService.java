package org.example.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.auth.client.TeamServiceClient;
import org.example.auth.client.UserDto;
import org.example.auth.dto.AuthResponse;
import org.example.auth.dto.LoginRequest;
import org.example.auth.dto.RegisterRequest;
import org.example.auth.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final TeamServiceClient teamServiceClient;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(LoginRequest request) {
        try {
            // Get user from team-service
            UserDto user = teamServiceClient.getUserByEmail(request.getEmail());
            
            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new RuntimeException("Invalid email or password");
            }
            
            // Generate tokens
            String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
            String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());
            
            log.info("User {} logged in successfully", user.getEmail());
            
            return new AuthResponse(
                    accessToken,
                    refreshToken,
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    user.getRole()
            );
        } catch (Exception e) {
            log.error("Login failed for email {}: {}", request.getEmail(), e.getMessage());
            throw new RuntimeException("Invalid email or password");
        }
    }

    public AuthResponse register(RegisterRequest request) {
        try {
            // Create user DTO
            UserDto userDto = new UserDto();
            userDto.setEmail(request.getEmail());
            userDto.setName(request.getName());
            userDto.setPassword(passwordEncoder.encode(request.getPassword()));
            userDto.setTeamId(request.getTeamId());
            userDto.setRole(request.getRole());
            
            // Create user in team-service
            UserDto createdUser = teamServiceClient.createUser(userDto);
            
            // Generate tokens
            String accessToken = jwtUtil.generateToken(createdUser.getId(), createdUser.getEmail(), createdUser.getRole());
            String refreshToken = jwtUtil.generateRefreshToken(createdUser.getId(), createdUser.getEmail());
            
            log.info("User {} registered successfully", createdUser.getEmail());
            
            return new AuthResponse(
                    accessToken,
                    refreshToken,
                    createdUser.getId(),
                    createdUser.getEmail(),
                    createdUser.getName(),
                    createdUser.getRole()
            );
        } catch (Exception e) {
            log.error("Registration failed for email {}: {}", request.getEmail(), e.getMessage());
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
    }

    public AuthResponse refreshToken(String refreshToken) {
        try {
            // Validate refresh token
            if (!jwtUtil.validateToken(refreshToken)) {
                throw new RuntimeException("Invalid refresh token");
            }
            
            // Extract user info
            String email = jwtUtil.extractEmail(refreshToken);
            Long userId = jwtUtil.extractUserId(refreshToken);
            
            // Get user to get current role
            UserDto user = teamServiceClient.getUserByEmail(email);
            
            // Generate new access token
            String newAccessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
            String newRefreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());
            
            log.info("Token refreshed for user {}", email);
            
            return new AuthResponse(
                    newAccessToken,
                    newRefreshToken,
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    user.getRole()
            );
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw new RuntimeException("Failed to refresh token");
        }
    }

    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }
}
