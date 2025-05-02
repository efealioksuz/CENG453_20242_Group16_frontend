package com.example.unofrontend.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import javafx.scene.Node;

import java.io.IOException;

@Component
public class GameModeDialogController {
    @FXML
    private Button singlePlayerButton;
    @FXML
    private Button multiPlayerButton;
    @FXML
    private Button goBackButton;

    @Autowired
    private ApplicationContext context;

    private Stage dialogStage;

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    @FXML
    private void handleSinglePlayer(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/GameBoard.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            GameBoardController gameBoardController = loader.getController();
            gameBoardController.initializeSinglePlayer();
            gameBoardController.setPlayerName(com.example.unofrontend.session.SessionManager.getUsername());
            Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
            
            // Get screen dimensions
            javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            
            // Create a new scene with screen dimensions
            Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
            stage.setScene(scene);
            
            // Position the window to fill the screen
            stage.setX(screenBounds.getMinX());
            stage.setY(screenBounds.getMinY());
            stage.setWidth(screenBounds.getWidth());
            stage.setHeight(screenBounds.getHeight());
            

            stage.setMaximized(true);
            
            javafx.application.Platform.runLater(() -> {
                // Double-check dimensions
                if (stage.getWidth() < screenBounds.getWidth() || stage.getHeight() < screenBounds.getHeight()) {
                    stage.setWidth(screenBounds.getWidth());
                    stage.setHeight(screenBounds.getHeight());
                    stage.setX(screenBounds.getMinX());
                    stage.setY(screenBounds.getMinY());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (dialogStage != null) dialogStage.close();
    }

    @FXML
    private void handleMultiPlayer(ActionEvent event) {
        // TODO: Implement multiplayer logic
        if (dialogStage != null) dialogStage.close();
    }

    @FXML
    private void handleGoBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/menu.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            Stage stage = (Stage) goBackButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (dialogStage != null) dialogStage.close();
    }
} 