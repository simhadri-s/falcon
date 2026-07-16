package com.laserxprts.falcon.dto.response;

import java.util.HashSet;
import java.util.Set;

import com.laserxprts.falcon.model.Role;
import com.laserxprts.falcon.model.User;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserResponse {
    private String email;
    private String name;
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
            .email(user.getEmail())
            .name(user.getName())
            .roles(user.getRoles())
            .build();
    }
}
