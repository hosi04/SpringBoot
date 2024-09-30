package com.example.identity_service.service;

import com.example.identity_service.dto.request.UserCreationRequest;
import com.example.identity_service.dto.request.UserUpdateRequest;
import com.example.identity_service.dto.response.UserResponse;
import com.example.identity_service.entity.Role;
import com.example.identity_service.entity.User;
import com.example.identity_service.exception.AppException;
import com.example.identity_service.exception.ErrorCode;
import com.example.identity_service.mapper.UserMapper;
import com.example.identity_service.repository.RoleRepository;
import com.example.identity_service.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) // Thay the AutoWide
@Slf4j
public class UserService {
    private final RoleRepository roleRepository;

    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

//    Function Create a User into SQL
public UserResponse createUser(UserCreationRequest request) {
    log.info("Service: Create User");

    if(userRepository.existsByUsername(request.getUsername()))
        throw new AppException(ErrorCode.USER_EXISTED);
    User user = userMapper.toUser(request);
    user.setPassword(passwordEncoder.encode(request.getPassword()));

    Role roleDefault = roleRepository.findById("USER").orElseThrow(() -> new RuntimeException("Role not found"));

    Set<Role> roles = new HashSet<>();
    roles.add(roleDefault);

    user.setRoles(roles);

    return userMapper.toUserResponse(userRepository.save(user));

}

//    Update thi minh lay dto request Create cung duoc, nhung thong thuong thi khong Update usernName nen tao 1 cai
//    giong nhu create nhung khong co userName
public UserResponse updateUser(String userId, UserUpdateRequest request) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    userMapper.updateUser(user, request);
    user.setPassword(passwordEncoder.encode(request.getPassword()));

    return userMapper.toUserResponse(userRepository.save(user));
}

    public void deleteUser(String userId){
        userRepository.deleteById(userId);
    }

//    Function get User
    @PreAuthorize("hasRole('ADMIN')") //Tao ra 1 proxy truoc cai ham nay, truoc khi goi ham nay thi phai kiem tra xem co phai Role ADMIN khong?
    public List<UserResponse> getUsers(){
        log.info("In method gerUsers");
        return userRepository.findAll().stream().map(userMapper::toUserResponse).toList();
    }

//    Funciton get User by ID
    @PostAuthorize("returnObject.username == authentication.name") // Goi method truoc va sau do kiem tra xem co phai Role mong muon hay khong moi tra ket qua?, authentication.name la USER hien tai
    public UserResponse getUser(String id){
        log.info("In method gerUsersByID");
        return  userMapper.toUserResponse(userRepository.findById(id). // Nếu tìm thấy thì trả về
                orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED))); // Còn không tìm thấy thì báo lỗi
    }

    public UserResponse getMyInfo(){
        var context = SecurityContextHolder.getContext(); //Get User hien tai
        String name = context.getAuthentication().getName(); // Username cua User dang nhap hien tai
        User user = userRepository.findByUsername(name).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return userMapper.toUserResponse(user);
    }
}
