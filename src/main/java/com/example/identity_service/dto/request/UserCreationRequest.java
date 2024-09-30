package com.example.identity_service.dto.request;

import com.example.identity_service.validator.DobConstraint;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE) // Moi cai field khong xac dinh thi mac dinh la PRIVATE
    public class UserCreationRequest {
        @Size(min = 4, message = "USERNAME_INVALID") //Api cua string bat buoc phai truyen vao const nen truyen String vao cho co thoi
        String username;
        @Size(min = 8, message = "PASSWORD_INVALID") // De tren cai nao thi no ap dung vo cai do
        String password;
        String firstName;
        String lastName;
        @DobConstraint(min = 14, message = "INVALID_DOB")
        LocalDate dob;
}
