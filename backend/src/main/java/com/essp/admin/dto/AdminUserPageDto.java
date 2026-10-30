package com.essp.admin.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminUserPageDto(
        List<AdminUserDto> content, int page, int size, long totalElements, int totalPages) {
    public static AdminUserPageDto from(Page<com.essp.security.entity.User> users) {
        return new AdminUserPageDto(users.getContent().stream().map(AdminUserDto::from).toList(),
                users.getNumber(), users.getSize(), users.getTotalElements(), users.getTotalPages());
    }
}
