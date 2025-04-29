package com.example.unofrontend.controllers;

import java.io.IOException;

import com.example.unofrontend.models.LoginRequest;
import com.example.unofrontend.services.ApiService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button registerButton;

    @FXML
    private Button resetPasswordButton;

    private final ApiService apiService = new ApiService();

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Username and Password cannot be empty!");
            errorLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        String result = apiService.login(new LoginRequest(username, password));

        if (result.equals("success")) {
            errorLabel.setText("Login successful!");
            errorLabel.setStyle("-fx-text-fill: green;");
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/GameBoard.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) usernameField.getScene().getWindow();
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.setMaximized(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            errorLabel.setText(result);
            errorLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void switchToRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/register.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void switchToResetPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/reset-password.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) resetPasswordButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}