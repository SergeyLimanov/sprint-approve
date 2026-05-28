package org.example.auth.service;

import org.example.auth.client.TeamServiceClient;
import org.example.auth.client.UserDto;
import org.example.auth.dto.AuthResponse;
import org.example.auth.dto.LoginRequest;
import org.example.auth.dto.RegisterRequest;
import org.example.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private TeamServiceClient teamServiceClient;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private UserDto testUser;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = new UserDto();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setPassword("$2a$10$encodedPassword");
        testUser.setRole("DEVELOPER");
        testUser.setTeamId(1L);

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setName("New User");
        registerRequest.setPassword("password123");
        registerRequest.setRole("DEVELOPER");
        registerRequest.setTeamId(1L);
    }

    @Test
    void testLoginSuccess() {
        // Given
        when(teamServiceClient.getUserByEmail(loginRequest.getEmail())).thenReturn(testUser);
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword())).thenReturn(true);
        when(jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), testUser.getRole()))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(testUser.getId(), testUser.getEmail()))
                .thenReturn("refresh-token");

        // When
        AuthResponse response = authService.login(loginRequest);

        // Then
        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals(testUser.getId(), response.getUserId());
        assertEquals(testUser.getEmail(), response.getEmail());
        assertEquals(testUser.getName(), response.getName());
        assertEquals(testUser.getRole(), response.getRole());

        verify(teamServiceClient).getUserByEmail(loginRequest.getEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), testUser.getPassword());
        verify(jwtTokenProvider).generateToken(testUser.getId(), testUser.getEmail(), testUser.getRole());
        verify(jwtTokenProvider).generateRefreshToken(testUser.getId(), testUser.getEmail());
    }

    @Test
    void testLoginFailureInvalidPassword() {
        // Given
        when(teamServiceClient.getUserByEmail(loginRequest.getEmail())).thenReturn(testUser);
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword())).thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(loginRequest);
        });

        assertEquals("Invalid email or password", exception.getMessage());
        verify(teamServiceClient).getUserByEmail(loginRequest.getEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), testUser.getPassword());
        verify(jwtTokenProvider, never()).generateToken(anyLong(), anyString(), anyString());
    }

    @Test
    void testLoginFailureUserNotFound() {
        // Given
        when(teamServiceClient.getUserByEmail(loginRequest.getEmail()))
                .thenThrow(new RuntimeException("User not found"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(loginRequest);
        });

        assertEquals("Invalid email or password", exception.getMessage());
        verify(teamServiceClient).getUserByEmail(loginRequest.getEmail());
        verify(jwtTokenProvider, never()).generateToken(anyLong(), anyString(), anyString());
    }

    @Test
    void testRegisterSuccess() {
        // Given
        UserDto newUser = new UserDto();
        newUser.setId(2L);
        newUser.setEmail(registerRequest.getEmail());
        newUser.setName(registerRequest.getName());
        newUser.setPassword("$2a$10$encodedPassword");
        newUser.setRole(registerRequest.getRole());
        newUser.setTeamId(registerRequest.getTeamId());

        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("$2a$10$encodedPassword");
        when(teamServiceClient.createUser(any(UserDto.class))).thenReturn(newUser);
        when(jwtTokenProvider.generateToken(newUser.getId(), newUser.getEmail(), newUser.getRole()))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(newUser.getId(), newUser.getEmail()))
                .thenReturn("refresh-token");

        // When
        AuthResponse response = authService.register(registerRequest);

        // Then
        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals(newUser.getId(), response.getUserId());
        assertEquals(newUser.getEmail(), response.getEmail());
        assertEquals(newUser.getName(), response.getName());
        assertEquals(newUser.getRole(), response.getRole());

        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(teamServiceClient).createUser(any(UserDto.class));
        verify(jwtTokenProvider).generateToken(newUser.getId(), newUser.getEmail(), newUser.getRole());
        verify(jwtTokenProvider).generateRefreshToken(newUser.getId(), newUser.getEmail());
    }

    @Test
    void testValidateTokenSuccess() {
        // Given
        String token = "valid-token";
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);

        // When
        Boolean isValid = authService.validateToken(token);

        // Then
        assertTrue(isValid);
        verify(jwtTokenProvider).validateToken(token);
    }

    @Test
    void testValidateTokenFailure() {
        // Given
        String token = "invalid-token";
        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        // When
        Boolean isValid = authService.validateToken(token);

        // Then
        assertFalse(isValid);
        verify(jwtTokenProvider).validateToken(token);
    }
}
