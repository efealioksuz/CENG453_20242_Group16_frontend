package com.example.unofrontend.session;

public class SessionManager {
    private static String token;
    private static String username;
    private static String userId;

    public static void setSession(String jwtToken, String userName, String userId) {
        token = jwtToken;
        username = userName;
        SessionManager.userId = userId;
    }

    public static String getToken() {
        return token;
    }

    public static String getUsername() {
        return username;
    }

    public static String getUserId() {
        return userId;
    }

    public static void clearSession() {
        token = null;
        username = null;
        userId = null;
    }
}