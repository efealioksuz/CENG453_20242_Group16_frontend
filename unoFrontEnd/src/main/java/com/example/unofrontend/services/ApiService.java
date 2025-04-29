package com.example.unofrontend.services;

import com.example.unofrontend.models.LoginRequest;
import com.example.unofrontend.models.LoginResponseDto;
import com.example.unofrontend.models.RegisterRequest;
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

@Service
public class ApiService {
    private static final Logger logger = LoggerFactory.getLogger(ApiService.class);

    @Value("${api.base-url:http://localhost:8080}")
    private String baseUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public String login(LoginRequest loginRequest) {
        try {
            logger.info("Attempting login for user: {}", loginRequest.getUsername());
            
            String url = baseUrl + "/auth/login";
            logger.info("Login URL: {}", url);
            
            // Set up headers for form data
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            // Create form data
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("username", loginRequest.getUsername());
            formData.add("password", loginRequest.getPassword());
            
            // Create request entity with form data
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);
            logger.info("Request headers: {}", headers);
            logger.info("Request body: username={}, password=***", loginRequest.getUsername());
            
            // Make POST request with form data
            ResponseEntity<LoginResponseDto> response = restTemplate.postForEntity(
                url,
                requestEntity,
                LoginResponseDto.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                LoginResponseDto loginResponse = response.getBody();
                // Store the token and username in session
                SessionManager.setSession(loginResponse.getToken(), loginResponse.getUser().getUsername());
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
            
            // Make POST request with query parameters
            ResponseEntity<String> response = restTemplate.postForEntity(
                url,
                null,  // No request body needed since we're using query parameters
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
            
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            // Create form data
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("email", email);
            
            // Create request entity
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);
            
            // Make POST request using exchange
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
            
            String url = baseUrl + "/auth/set-new-password";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> request = new HttpEntity<>(newPassword, headers);
            
            String response = restTemplate.postForObject(url, request, String.class);
            
            if (response != null && response.equals("success")) {
                logger.info("Password reset successful");
                return "success";
            }
            
            return response;
        } catch (Exception e) {
            logger.error("Error during password reset", e);
            return "An error occurred while resetting password. Please try again.";
        }
    }
}