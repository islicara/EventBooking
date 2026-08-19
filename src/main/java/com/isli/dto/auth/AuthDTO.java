package com.isli.dto.auth;

import com.isli.entity.Role;
import lombok.Builder;

@Builder
public class AuthDTO {

    private String token;
    private String username;
    private Role role;

    public AuthDTO() {
    }

    public AuthDTO(String token, String username, Role role) {
        this.token = token;
        this.username = username;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}