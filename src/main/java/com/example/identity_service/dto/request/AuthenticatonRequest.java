package com.example.identity_service.dto.request;

import org.springframework.web.bind.annotation.RestController;

import lombok.*;
import lombok.experimental.FieldDefaults;

@RestController
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticatonRequest {
    // User cung cấp username và password để đăng nhập
    String username;
    String password;
}
