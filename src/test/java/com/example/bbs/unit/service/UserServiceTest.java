package com.example.bbs.unit.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.bbs.repository.UserRepository;
import com.example.bbs.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void isUsernameTaken_存在するユーザー名の場合_trueを返す() {
        // Arrange
        when(userRepository.existsByUsername("junya")).thenReturn(true);

        // Act
        boolean result = userService.isUsernameTaken("junya");

        // Assert
        assertTrue(result);
    }

    @Test
    void isUsernameTaken_存在しないユーザー名の場合_falseを返す() {
        // Arrange
        when(userRepository.existsByUsername("junya")).thenReturn(false);

        // Act
        boolean result = userService.isUsernameTaken("junya");

        // Assert
        assertFalse(result);
    }
}