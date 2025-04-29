package com.example.unofrontend.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.example.unofrontend.models.LoginRequest;
import com.example.unofrontend.models.LoginResponse;
import com.example.unofrontend.models.RegisterRequest;
import com.example.unofrontend.session.SessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ApiService {

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String login(LoginRequest loginRequest) {
        try {
            String url = String.format("http://localhost:8080/auth/login?username=%s&password=%s",
                    loginRequest.getUsername(), loginRequest.getPassword());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("accept", "*/*")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            System.out.println("Sending request to: " + request.uri());

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Response status: " + response.statusCode());
            System.out.println("Response body: " + response.body());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                LoginResponse parsedResponse = objectMapper.readValue(responseBody, LoginResponse.class);

                String token = parsedResponse.getToken();
                String username = parsedResponse.getUser().getUsername();

                SessionManager.setSession(token, username);
                System.out.println("Token saved: " + token);
                return "success";
            } else {
                return response.body();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "An error occurred during login. Please try again.";
        }
    }

    public String register(RegisterRequest registerRequest) {
        try {
            String url = String.format("http://localhost:8080/auth/register?username=%s&email=%s&password=%s",
                    registerRequest.getUsername(),
                    registerRequest.getEmail(),
                    registerRequest.getPassword());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("accept", "*/*")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            System.out.println("Sending registration request to: " + request.uri());

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Response status: " + response.statusCode());
            System.out.println("Response body: " + response.body());

            if (response.statusCode() == 200) {
                return "success";
            } else {
                return response.body();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "An error occurred during registration. Please try again.";
        }
    }

    public String requestPasswordReset(String email) {
        try {
            String url = String.format("http://localhost:8080/auth/reset-password?email=%s", email);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("accept", "*/*")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            System.out.println("Sending password reset request to: " + request.uri());

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Response status: " + response.statusCode());
            System.out.println("Response body: " + response.body());

            if (response.statusCode() == 200) {
                return "success";
            } else {
                return response.body();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "An error occurred while requesting password reset. Please try again.";
        }
    }

    public String resetPassword(String token, String newPassword) {
        try {
            String url = String.format("http://localhost:8080/auth/set-new-password?token=%s&newPassword=%s",
                    token, newPassword);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("accept", "*/*")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            System.out.println("Sending password reset confirmation to: " + request.uri());

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Response status: " + response.statusCode());
            System.out.println("Response body: " + response.body());

            if (response.statusCode() == 200) {
                return "success";
            } else {
                return response.body();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "An error occurred while resetting password. Please try again.";
        }
    }
}