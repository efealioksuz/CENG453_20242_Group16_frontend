package com.example.unofrontend.models;

import lombok.Data;

@Data
public class LoginResponseDto {
    private String token;
    private UserDto user;

    @Data
    public static class UserDto {
        private String username;
        private String email;
    }
} 