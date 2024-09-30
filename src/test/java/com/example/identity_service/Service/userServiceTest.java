package com.example.identity_service.Service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.example.identity_service.dto.request.UserCreationRequest;
import com.example.identity_service.dto.response.UserResponse;
import com.example.identity_service.entity.User;
import com.example.identity_service.exception.AppException;
import com.example.identity_service.repository.UserRepository;
import com.example.identity_service.service.UserService;

@SpringBootTest
public class userServiceTest {
    @Autowired
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    private UserCreationRequest request;
    private UserResponse userResponse;
    private User user;
    private LocalDate dob;

    @BeforeEach
    // Sẽ được chạy trước khi chạy testcase
    void initData() {
        dob = LocalDate.of(1990, 1, 1);

        request = UserCreationRequest.builder()
                .username("john")
                .firstName("john")
                .lastName("john")
                .password("1234567890")
                .dob(dob)
                .build();

        userResponse = UserResponse.builder()
                .id("15482bcb1f3b")
                .username("john")
                .firstName("john")
                .lastName("Doe")
                .dob(dob)
                .build();

        user = User.builder()
                .id("15482bcb1f3b")
                .username("john")
                .firstName("john")
                .lastName("Doe")
                .dob(dob)
                .build();
    }

    @Test
    void createUser_validRequest_success() {
        // GIVEN
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.save(any())).thenReturn(user);
        // WHEN
        var response = userService.createUser(request);
        // THEN
        Assertions.assertThat(response.getId()).isEqualTo("15482bcb1f3b");
        Assertions.assertThat(response.getUsername()).isEqualTo("john");
    }

    @Test
    void createUser_userExsisted_false() {
        // GIVEN
        when(userRepository.existsByUsername(anyString())).thenReturn(true);
        // WHEN
        var exception = assertThrows(AppException.class, () -> userService.createUser(request));
        // THEN
        Assertions.assertThat(exception.getErrorCode().getCode()).isEqualTo(1001);
    }
}
