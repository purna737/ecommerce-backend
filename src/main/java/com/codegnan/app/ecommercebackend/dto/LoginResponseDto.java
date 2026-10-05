package com.codegnan.app.ecommercebackend.dto;

import java.util.Set;

public class LoginResponseDto {

    private String message;
    private Long userId;
    private String username;
    private String firstName;
    private Set<String> roles;

    public LoginResponseDto() {
    }

    public LoginResponseDto(
            String message,
            Long userId,
            String username,
            String firstName,
            Set<String> roles) {

        this.message = message;
        this.userId = userId;
        this.username = username;
        this.firstName = firstName;
        this.roles = roles;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}