package com.frauddetection.frauddetection.dto;

import com.frauddetection.frauddetection.entity.User;

/**
 * Public administrative DTO for user identity details.
 * Strictly excludes sensitive security fields such as password hashes.
 */
public record AdminUserDetailDto(
        Long id,
        String username,
        String email,
        String role
) {
    public static AdminUserDetailDto fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return new AdminUserDetailDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
    }
}
