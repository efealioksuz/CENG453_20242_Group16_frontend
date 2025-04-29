package com.example.unofrontend.session;

public class SessionManager {
    private static String token;
    private static String username;

    public static void setSession(String jwtToken, String userName) {
        token = jwtToken;
        username = userName;
    }

    public static String getToken() {
        return token;
    }

    public static String getUsername() {
        return username;
    }

    public static void clearSession() {
        token = null;
        username = null;
    }
}