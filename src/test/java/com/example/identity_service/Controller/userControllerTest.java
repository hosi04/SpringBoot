package com.example.identity_service.Controller;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.example.identity_service.dto.request.UserCreationRequest;
import com.example.identity_service.dto.response.UserResponse;
import com.example.identity_service.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc // Tạo 1 req giả gởi đến Controller
public class userControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private UserCreationRequest request;
    private UserResponse userResponse;
    private LocalDate dob;

    @BeforeEach // Sẽ được chạy trước khi chạy testcase
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
    }
    // Code bên dưới là 1 testcase
    @Test
    // Ghi chu
    void createUser_validRequest_success() throws Exception {
        // GIVEN
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String content = objectMapper.writeValueAsString(request);

        Mockito.when(userService.createUser(ArgumentMatchers.any()))
                .thenReturn(userResponse); // Thay vì gọi userService thì sẽ trả kết quả trực tiếp là usserResponse luôn

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.post("/users")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isOk()) // KHÚC THEN
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(1000))
                .andExpect(MockMvcResultMatchers.jsonPath("result.id").value("15482bcb1f3b"));
    }

    @Test
    void createUser_usernameInvalid_false() throws Exception {
        // GIVEN
        request.setUsername("Jo");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String content = objectMapper.writeValueAsString(request);

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.post("/users")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isBadRequest()) // KHÚC THEN
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(1002))
                .andExpect(MockMvcResultMatchers.jsonPath("message").value("Username must be at least 3 characters"));
    }
}
