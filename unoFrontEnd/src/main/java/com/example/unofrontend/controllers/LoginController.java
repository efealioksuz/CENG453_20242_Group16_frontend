package com.example.unofrontend.controllers;

import com.example.unofrontend.models.LoginRequest;
import com.example.unofrontend.services.ApiService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    private final ApiService apiService = new ApiService();

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Username and Password cannot be empty!");
            return;
        }

        boolean loginSuccessful = apiService.login(new LoginRequest(username, password));

        if (loginSuccessful) {
            errorLabel.setText("Login successful!");

        } else {
            errorLabel.setText("Invalid credentials.");
        }
    }
}