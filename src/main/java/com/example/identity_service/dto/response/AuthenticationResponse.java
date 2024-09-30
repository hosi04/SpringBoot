package com.example.identity_service.dto.response;

import org.springframework.web.bind.annotation.RestController;

import lombok.*;
import lombok.experimental.FieldDefaults;

@RestController
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationResponse {
    String token; // Tra ve token cho user
    boolean authenticated; // Biến này có giá trị true nếu User cung cấp thông tin đăng nhập đúng
}
