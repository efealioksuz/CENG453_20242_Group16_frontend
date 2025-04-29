package com.example.unofrontend.services;

import com.example.unofrontend.models.LoginRequest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.example.unofrontend.models.LoginResponse;
import com.example.unofrontend.session.SessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ApiService {

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean login(LoginRequest loginRequest) {
        try {
            String requestBody = String.format(
                    "{\"username\":\"%s\", \"password\":\"%s\"}",
                    loginRequest.getUsername(), loginRequest.getPassword()
            );

            String url = String.format("http://localhost:8080/auth/login?username=%s&password=%s",
                    loginRequest.getUsername(), loginRequest.getPassword());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("accept", "*/*")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            System.out.println("Sending request to: " + request.uri());
            System.out.println("Request body: " + requestBody);

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
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}