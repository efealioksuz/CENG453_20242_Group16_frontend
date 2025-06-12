package com.example.unofrontend.services;

import com.example.unofrontend.models.LoginRequest;
import com.example.unofrontend.models.LoginResponse;
import com.example.unofrontend.models.RegisterRequest;
import com.example.unofrontend.models.Leaderboard;
import com.example.unofrontend.session.SessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.HttpMethod;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class ApiService {
    private static final Logger logger = LoggerFactory.getLogger(ApiService.class);

    @Value("${api.base-url:https://ceng453-20242-group16-backend.onrender.com/}")
    private String baseUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public String getBaseUrl() {
        return baseUrl;
    }

    public String login(LoginRequest loginRequest) {
        try {
            logger.info("Attempting login for user: {}", loginRequest.getUsername());
            String url = baseUrl + "/auth/login";
            logger.info("Login URL: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("username", loginRequest.getUsername());
            formData.add("password", loginRequest.getPassword());

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);
            logger.info("Request headers: {}", headers);
            logger.info("Request body: username={}, password=***", loginRequest.getUsername());

            ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                url,
                requestEntity,
                LoginResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                LoginResponse loginResponse = response.getBody();
                // Store the token, username and userId in session
                SessionManager.setSession(
                    loginResponse.getToken(), 
                    loginResponse.getUser().getUsername(),
                    loginResponse.getUser().getId()
                );
                logger.info("Login successful for user: {}", loginResponse.getUser().getUsername());
                return "success";
            } else {
                logger.warn("Login failed for user: {}", loginRequest.getUsername());
                return "Login failed: Invalid credentials";
            }
        } catch (Exception e) {
            logger.error("Error during login", e);
            String errorMessage = e.getMessage();
            if (e instanceof HttpClientErrorException.Unauthorized) {
                logger.error("Unauthorized error response body: {}", ((HttpClientErrorException.Unauthorized) e).getResponseBodyAsString());
                return "Login failed: Invalid username or password";
            }
            return "Login failed: " + errorMessage;
        }
    }

    public String register(RegisterRequest registerRequest) {
        try {
            logger.info("Attempting registration for user: {}", registerRequest.getUsername());
            
            String url = baseUrl + "/auth/register" +
                "?username=" + registerRequest.getUsername() +
                "&email=" + registerRequest.getEmail() +
                "&password=" + registerRequest.getPassword();
            
            logger.info("Registration URL: {}", url);
            

            ResponseEntity<String> response = restTemplate.postForEntity(
                url,
                null,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.info("Registration successful for user: {}", registerRequest.getUsername());
                return "success";
            }
            
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error during registration", e);
            if (e instanceof HttpClientErrorException.BadRequest) {
                logger.error("Bad request error response body: {}", ((HttpClientErrorException.BadRequest) e).getResponseBodyAsString());
                return ((HttpClientErrorException.BadRequest) e).getResponseBodyAsString();
            }
            return "An error occurred during registration. Please try again.";
        }
    }

    public String requestPasswordReset(String email) {
        try {
            logger.info("Requesting password reset for email: {}", email);
            
            String url = baseUrl + "/auth/reset-password";
            logger.info("Password reset URL: {}", url);
            

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("email", email);
            

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);
            

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.info("Password reset request successful for email: {}", email);
                return "success";
            }
            
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error during password reset request", e);
            if (e instanceof HttpClientErrorException.BadRequest) {
                logger.error("Bad request error response body: {}", ((HttpClientErrorException.BadRequest) e).getResponseBodyAsString());
                return ((HttpClientErrorException.BadRequest) e).getResponseBodyAsString();
            }
            return "An error occurred while requesting password reset. Please try again.";
        }
    }

    public String resetPassword(String token, String newPassword) {
        try {
            logger.info("Resetting password with token");
            
            String url = baseUrl + "/auth/set-new-password" +
                "?token=" + token +
                "&newPassword=" + newPassword;
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                url,
                null, 
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Password reset successful");
                return "success";
            }
            
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error during password reset", e);
            return "An error occurred while resetting password. Please try again.";
        }
    }

    private HttpHeaders createAuthHeadersWithToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "*/*");
        String token = SessionManager.getToken();
        if (token != null && !token.isEmpty()) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    public List<Leaderboard> getWeeklyLeaderboard() {
        try {
            logger.info("Fetching weekly leaderboard");
            String url = baseUrl + "/weekly-leaderboard";
            HttpEntity<?> requestEntity = new HttpEntity<>(createAuthHeadersWithToken());
            ResponseEntity<List<Leaderboard>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<List<Leaderboard>>() {}
            );
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.info("Successfully retrieved weekly leaderboard");
                return response.getBody();
            }
            throw new RuntimeException("Failed to fetch weekly leaderboard");
        } catch (Exception e) {
            logger.error("Error fetching weekly leaderboard", e);
            throw new RuntimeException("Failed to fetch weekly leaderboard: " + e.getMessage());
        }
    }

    public List<Leaderboard> getMonthlyLeaderboard() {
        try {
            logger.info("Fetching monthly leaderboard");
            String url = baseUrl + "/monthly-leaderboard";
            HttpEntity<?> requestEntity = new HttpEntity<>(createAuthHeadersWithToken());
            ResponseEntity<List<Leaderboard>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<List<Leaderboard>>() {}
            );
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.info("Successfully retrieved monthly leaderboard");
                return response.getBody();
            }
            throw new RuntimeException("Failed to fetch monthly leaderboard");
        } catch (Exception e) {
            logger.error("Error fetching monthly leaderboard", e);
            throw new RuntimeException("Failed to fetch monthly leaderboard: " + e.getMessage());
        }
    }

    public List<Leaderboard> getAllTimeLeaderboard() {
        try {
            logger.info("Fetching all-time leaderboard");
            String url = baseUrl + "/all-time-leaderboard";
            HttpEntity<?> requestEntity = new HttpEntity<>(createAuthHeadersWithToken());
            ResponseEntity<List<Leaderboard>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<List<Leaderboard>>() {}
            );
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.info("Successfully retrieved all-time leaderboard");
                return response.getBody();
            }
            throw new RuntimeException("Failed to fetch all-time leaderboard");
        } catch (Exception e) {
            logger.error("Error fetching all-time leaderboard", e);
            throw new RuntimeException("Failed to fetch all-time leaderboard: " + e.getMessage());
        }
    }

    public String updateDailyScore(int score) {
        try {
            String userId = SessionManager.getUserId();
            if (userId == null) {
                logger.error("Cannot update score: User ID is not set");
                return "Cannot update score: User is not properly logged in";
            }

            logger.info("Updating daily score for user ID: {} with score: {}", userId, score);
            String url = baseUrl + "/daily-score?userId=" + userId + "&score=" + score;

            HttpHeaders headers = createAuthHeadersWithToken();
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                requestEntity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Daily score update successful for user ID: {}", userId);
                return "success";
            }

            return response.getBody();
        } catch (Exception e) {
            logger.error("Error updating daily score", e);
            return "An error occurred while updating daily score: " + e.getMessage();
        }
    }

    // Room Management Methods
    public Map<String, Object> createRoom(String playerName) {
        try {
            logger.info("Creating room for player: {}", playerName);
            
            String url = baseUrl + "/api/game/create-room";
            
            HttpHeaders headers = createAuthHeadersWithToken();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, String> request = new HashMap<>();
            request.put("playerName", playerName);
            
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                url,
                requestEntity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.info("Room created successfully");
                return (Map<String, Object>) response.getBody();
            }
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to create room");
            return errorResponse;
            
        } catch (Exception e) {
            logger.error("Error creating room", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error creating room: " + e.getMessage());
            return errorResponse;
        }
    }
    
    public Map<String, Object> joinRoom(String roomId, String playerName) {
        try {
            logger.info("Joining room {} for player: {}", roomId, playerName);
            
            String url = baseUrl + "/api/game/join-room";
            
            HttpHeaders headers = createAuthHeadersWithToken();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, String> request = new HashMap<>();
            request.put("roomId", roomId);
            request.put("playerName", playerName);
            
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                url,
                requestEntity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.info("Joined room successfully");
                return (Map<String, Object>) response.getBody();
            }
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to join room");
            return errorResponse;
            
        } catch (Exception e) {
            logger.error("Error joining room", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error joining room: " + e.getMessage());
            return errorResponse;
        }
    }
    
    public Map<String, Object> getRoomStatus(String roomId) {
        try {
            logger.info("Getting status for room: {}", roomId);
            
            String url = baseUrl + "/api/game/room/" + roomId + "/status";
            
            HttpHeaders headers = createAuthHeadersWithToken();
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.info("Room status retrieved successfully");
                return (Map<String, Object>) response.getBody();
            }
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get room status");
            return errorResponse;
            
        } catch (Exception e) {
            logger.error("Error getting room status", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error getting room status: " + e.getMessage());
            return errorResponse;
        }
    }
}