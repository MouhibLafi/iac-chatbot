package com.company.iacchatbot.service;

import com.company.iacchatbot.dto.QuotaRequest;
import com.company.iacchatbot.dto.UserDto;
import com.company.iacchatbot.model.Role;
import com.company.iacchatbot.model.User;
import com.company.iacchatbot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du UserService
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService();
        ReflectionTestUtils.setField(userService, "userRepository", userRepository);
    }

    private User createTestUser(Long id, String username, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("encodedPassword");
        user.setRole(role);
        user.setEnabled(true);
        user.setQuotaCpu(32);
        user.setQuotaRam(128);
        user.setQuotaStorage(1000);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    void testGetAllUsers() {
        // Arrange
        User user1 = createTestUser(1L, "admin", "admin@test.com", Role.ADMIN);
        User user2 = createTestUser(2L, "user", "user@test.com", Role.USER);

        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        // Act
        List<UserDto> results = userService.getAllUsers();

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("admin", results.get(0).getUsername());
        assertEquals("user", results.get(1).getUsername());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void testGetAllUsersEmpty() {
        // Arrange
        when(userRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<UserDto> results = userService.getAllUsers();

        // Assert
        assertNotNull(results);
        assertEquals(0, results.size());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void testGetUserById() {
        // Arrange
        User user = createTestUser(1L, "admin", "admin@test.com", Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        UserDto result = userService.getUserById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("admin", result.getUsername());
        assertEquals("admin@test.com", result.getEmail());
        assertEquals("ADMIN", result.getRole());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void testGetUserByIdNotFound() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.getUserById(99L));
        verify(userRepository, times(1)).findById(99L);
    }

    @Test
    void testGetUserByUsername() {
        // Arrange
        User user = createTestUser(1L, "admin", "admin@test.com", Role.ADMIN);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        // Act
        UserDto result = userService.getUserByUsername("admin");

        // Assert
        assertNotNull(result);
        assertEquals("admin", result.getUsername());
        assertEquals("admin@test.com", result.getEmail());
        verify(userRepository, times(1)).findByUsername("admin");
    }

    @Test
    void testGetUserByUsernameNotFound() {
        // Arrange
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.getUserByUsername("unknown"));
        verify(userRepository, times(1)).findByUsername("unknown");
    }

    @Test
    void testDeleteUser() {
        // Arrange
        User user = createTestUser(1L, "admin", "admin@test.com", Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        userService.deleteUser(1L);

        // Assert
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).delete(user);
    }

    @Test
    void testDeleteUserNotFound() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.deleteUser(99L));
        verify(userRepository, never()).delete(any());
    }

    @Test
    void testToggleUserEnabledToDisabled() {
        // Arrange
        User user = createTestUser(1L, "admin", "admin@test.com", Role.ADMIN);
        user.setEnabled(true);

        User disabledUser = createTestUser(1L, "admin", "admin@test.com", Role.ADMIN);
        disabledUser.setEnabled(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(disabledUser);

        // Act
        UserDto result = userService.toggleUserEnabled(1L);

        // Assert
        assertNotNull(result);
        assertFalse(result.getEnabled());
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testToggleUserEnabledToEnabled() {
        // Arrange
        User user = createTestUser(1L, "admin", "admin@test.com", Role.ADMIN);
        user.setEnabled(false);

        User enabledUser = createTestUser(1L, "admin", "admin@test.com", Role.ADMIN);
        enabledUser.setEnabled(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(enabledUser);

        // Act
        UserDto result = userService.toggleUserEnabled(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.getEnabled());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testUpdateQuotaCpu() {
        // Arrange
        User user = createTestUser(1L, "admin", "admin@test.com", Role.ADMIN);
        user.setQuotaCpu(32);

        User updatedUser = createTestUser(1L, "admin", "admin@test.com", Role.ADMIN);
        updatedUser.setQuotaCpu(64);

        QuotaRequest quotaRequest = new QuotaRequest();
        quotaRequest.setQuotaCpu(64);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        UserDto result = userService.updateQuota(1L, quotaRequest);

        // Assert
        assertNotNull(result);
        assertEquals(64, updatedUser.getQuotaCpu());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testUpdateQuotaRam() {
        // Arrange
        User user = createTestUser(1L, "admin", "admin@test.com", Role.ADMIN);
        user.setQuotaRam(128);

        User updatedUser = createTestUser(1L, "admin", "admin@test.com", Role.ADMIN);
        updatedUser.setQuotaRam(256);

        QuotaRequest quotaRequest = new QuotaRequest();
        quotaRequest.setQuotaRam(256);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        UserDto result = userService.updateQuota(1L, quotaRequest);

        // Assert
        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testUpdateQuotaStorage() {
        // Arrange
        User user = createTestUser(1L, "admin", "admin@test.com", Role.ADMIN);
        user.setQuotaStorage(1000);

        User updatedUser = createTestUser(1L, "admin", "admin@test.com", Role.ADMIN);
        updatedUser.setQuotaStorage(2000);

        QuotaRequest quotaRequest = new QuotaRequest();
        quotaRequest.setQuotaStorage(2000);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        UserDto result = userService.updateQuota(1L, quotaRequest);

        // Assert
        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testUpdateMultipleQuotas() {
        // Arrange
        User user = createTestUser(1L, "admin", "admin@test.com", Role.ADMIN);

        User updatedUser = createTestUser(1L, "admin", "admin@test.com", Role.ADMIN);
        updatedUser.setQuotaCpu(64);
        updatedUser.setQuotaRam(256);
        updatedUser.setQuotaStorage(2000);

        QuotaRequest quotaRequest = new QuotaRequest();
        quotaRequest.setQuotaCpu(64);
        quotaRequest.setQuotaRam(256);
        quotaRequest.setQuotaStorage(2000);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        UserDto result = userService.updateQuota(1L, quotaRequest);

        // Assert
        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testUpdateQuotaUserNotFound() {
        // Arrange
        QuotaRequest quotaRequest = new QuotaRequest();
        quotaRequest.setQuotaCpu(64);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.updateQuota(99L, quotaRequest));
        verify(userRepository, never()).save(any());
    }
}
