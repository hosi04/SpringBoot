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
public class IntrospectResponse {
    boolean valid; // De xac dinh token con hieu luc hay khong
}
