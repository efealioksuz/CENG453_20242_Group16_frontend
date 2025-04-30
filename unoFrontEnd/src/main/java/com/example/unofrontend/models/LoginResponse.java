package com.example.unofrontend.models;

public class LoginResponse {
    private String token;
    private User user;

    public String getToken() {
        return token;
    }

    public User getUser() {
        return user;
    }

    public LoginResponse() {
    }
}

