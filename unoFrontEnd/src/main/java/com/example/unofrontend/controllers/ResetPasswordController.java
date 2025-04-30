package com.example.unofrontend.controllers;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import com.example.unofrontend.services.ApiService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class ResetPasswordController {

    @FXML
    private TextField emailField;

    @FXML
    private TextField tokenField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private Button sendButton;

    @FXML
    private Button submitButton;

    @FXML
    private Button backToLoginButton;

    @FXML
    private Label errorLabel;

    @FXML
    private VBox tokenSection;

    @Autowired
    private ApiService apiService;

    @Autowired
    private ApplicationContext context;

    @FXML
    private void initialize() {
        
        tokenSection.setVisible(false);
    }

    @FXML
    private void handleSendEmail() {
        String email = emailField.getText();

        if (email.isEmpty()) {
            errorLabel.setText("Email cannot be empty!");
            errorLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        String result = apiService.requestPasswordReset(email);

        if (result.equals("success")) {
            errorLabel.setText("Password reset email sent! Please check your mail inbox.");
            errorLabel.setStyle("-fx-text-fill: green;");
        
            tokenSection.setVisible(true);
        } else {
            errorLabel.setText(result);
            errorLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleSubmit() {
        String token = tokenField.getText();
        String newPassword = newPasswordField.getText();

        if (token.isEmpty() || newPassword.isEmpty()) {
            errorLabel.setText("Token and new password cannot be empty!");
            errorLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        String result = apiService.resetPassword(token, newPassword);

        if (result.equals("success")) {
            errorLabel.setText("Password reset successful! Redirecting to login page...");
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