package com.example.identity_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL) //Nhung cai nao co gia tri null thi no khong hien thi trong obj tra ve
public class ApiResponse <T> {
    @Builder.Default
    int code = 1000;
    String message;
    T result;

}
