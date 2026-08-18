package com.parkcar.security;

import lombok.Data;

import java.util.Set;

/**
 * 当前登录用户
 */
@Data
public class LoginUser {

    private Long id;
    private String username;
    private String realName;
    private Set<String> roles;

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
