package com.example.identity_service.repository;

import com.example.identity_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    //Khi gọi method này thì JPA của Spring nó tự tạo 1 query để check xem userName đã tồn tại hay chưa?
    boolean existsByUsername(String username);
    Optional<User> findByUsername(String username); //Khi tao ra method findByUsername thi api spring tu dong tim Username cua entity
}
