package com.example.unofrontend.controllers;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import com.example.unofrontend.models.RegisterRequest;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class RegisterController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField emailField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button backToLoginButton;

    @Autowired
    private ApiService apiService;

    @Autowired
    private ApplicationContext context;

    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String email = emailField.getText();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            errorLabel.setText("All fields are required!");
            errorLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        String result = apiService.register(new RegisterRequest(username, password, email));

        if (result.equals("success")) {
            errorLabel.setText("Registration successful! Redirecting to login page...");
            errorLabel.setStyle("-fx-text-fill: green;");
      
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    javafx.application.Platform.runLater(() -> {
                        switchToLogin();
                    });
                }
            }, 2000);
        } else {
            errorLabel.setText(result);
            errorLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void switchToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/login.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            Stage stage = (Stage) backToLoginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 