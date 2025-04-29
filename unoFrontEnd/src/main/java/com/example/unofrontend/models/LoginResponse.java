package com.example.unofrontend.models;


public class LoginResponse {
    private String token;
    private UserDto user;

    public String getToken() {
        return token;
    }

    public UserDto getUser() {
        return user;
    }

    public LoginResponse() {
    }
}

