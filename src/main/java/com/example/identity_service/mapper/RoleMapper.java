package com.example.identity_service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.identity_service.dto.request.RoleRequest;
import com.example.identity_service.dto.response.RoleResponse;
import com.example.identity_service.entity.Role;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "permissions", ignore = true) // Khi map thi se bo qua cai atribute Permission ra
    Role toRole(RoleRequest request); // Map roleReq vao Role

    RoleResponse toRoleResponse(Role role);
}
