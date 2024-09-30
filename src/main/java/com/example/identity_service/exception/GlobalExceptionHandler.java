package com.example.identity_service.exception;

import java.util.Map;
import java.util.Objects;

import jakarta.validation.ConstraintViolation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.example.identity_service.dto.response.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
// Class này khai báo ra để khi có 1 exception nào đó xảy ra thì class này sẽ chịu trách nhiệm xử lý
public class GlobalExceptionHandler {
    private static final String MIN_ATTRIBUTE = "min";
    // Tương ứng với từng exception là các method khác nhau
    @ExceptionHandler(value = Exception.class) // Danh cho cac expection chua duoc phan loai
    ResponseEntity<ApiResponse> handlingRuntimeException(RuntimeException exception) {
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(ErrorCode.UNCATEGORI_EXCEPTION.getCode());
        apiResponse.setMessage(ErrorCode.UNCATEGORI_EXCEPTION.getMessage());
        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(value = AppException.class) // Exception da phan loai
    ResponseEntity<ApiResponse> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = AccessDeniedException.class) // Bắt và xử lý Exception Denied
    ResponseEntity<ApiResponse> handlingAccessDeniedException(AccessDeniedException exception) {

        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        return ResponseEntity.status(errorCode.getStatusCode())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    // Xu ly username, password, dob
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse> handlingValidation(
            MethodArgumentNotValidException exception) { // Bắt và xử lý Exception Not Valid
        String enumKey = exception.getFieldError().getDefaultMessage();

        ErrorCode errorCode = ErrorCode.INVALID_KEY;
        Map<String, Objects> attributes = null;

        try {
            errorCode = ErrorCode.valueOf(enumKey); // Tao ra doi tuong ErrorCode(code, message)

            var constraintViolation =
                    exception.getBindingResult().getAllErrors().get(0).unwrap(ConstraintViolation.class);
            attributes = constraintViolation.getConstraintDescriptor().getAttributes();

            log.info(attributes.toString());

        } catch (IllegalArgumentException exception1) {
            // Tam thoi chua xu li daon nay
        }
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(
                Objects.nonNull(attributes)
                        ? mapAttribute(errorCode.getMessage(), attributes)
                        : errorCode.getMessage());
        return ResponseEntity.badRequest().body(apiResponse);
    }

    private String mapAttribute(String message, Map<String, Objects> attribute) {
        String minValue = String.valueOf(attribute.get(MIN_ATTRIBUTE));

        return message.replace("{" + MIN_ATTRIBUTE + "}", minValue);
    }
}
