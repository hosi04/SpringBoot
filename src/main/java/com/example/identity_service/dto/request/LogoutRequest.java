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
public class LogoutRequest {
    // Nhan vao 1 string token
    String token;
}
