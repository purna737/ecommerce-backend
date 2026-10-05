package com.codegnan.app.ecommercebackend.dto;

public class SignInRequestDto {

    private String username;
    private String password;

    public SignInRequestDto() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}