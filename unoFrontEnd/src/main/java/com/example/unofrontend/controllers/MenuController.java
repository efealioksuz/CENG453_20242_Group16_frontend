package com.example.unofrontend.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Alert;
import javafx.scene.control.cell.PropertyValueFactory;
import com.example.unofrontend.models.LeaderboardEntry;
import com.example.unofrontend.models.Leaderboard;
import com.example.unofrontend.services.ApiService;

import java.io.IOException;

@Component
public class MenuController {

    @FXML
    private Button newGameButton;

    @FXML
    private Button singlePlayerButton;

    @FXML
    private Button multiPlayerButton;

    @FXML
    private Button logoutButton;

    @FXML
    private Button leaderboardButton;

    @FXML
    private VBox gameModeBox;

    @FXML
    private StackPane gameModeOverlay;
    @FXML
    private Button closeGameModeOverlayButton;
    @FXML
    private Button singlePlayerOverlayButton;
    @FXML
    private Button multiPlayerOverlayButton;

    @FXML
    private VBox menuVBox;

    @FXML
    private StackPane leaderboardOverlay;
    @FXML
    private Button closeLeaderboardOverlayButton;
    @FXML
    private Button backToMenuFromLeaderboardButton;

    @FXML
    private TableView<LeaderboardEntry> leaderboardTable;
    @FXML
    private TableColumn<LeaderboardEntry, Integer> rankColumn;
    @FXML
    private TableColumn<LeaderboardEntry, String> usernameColumn;
    @FXML
    private TableColumn<LeaderboardEntry, Double> scoreColumn;
    @FXML
    private ComboBox<String> periodComboBox;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ApiService apiService;

    private final ObservableList<LeaderboardEntry> leaderboardData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            if (menuVBox != null) {
                menuVBox.requestLayout();
                menuVBox.layout();
            }
            if (leaderboardTable != null) {
                leaderboardTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            }
            if (periodComboBox != null) {
                periodComboBox.setItems(FXCollections.observableArrayList("All Time", "Monthly", "Weekly"));
                periodComboBox.setValue("All Time");
                periodComboBox.setOnAction(e -> loadLeaderboardData());
            }
            if (rankColumn != null) rankColumn.setCellValueFactory(new PropertyValueFactory<>("rank"));
            if (usernameColumn != null) usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
            if (scoreColumn != null) scoreColumn.setCellValueFactory(new PropertyValueFactory<>("score"));
        });
    }

    @FXML
    private void handleNewGame() {
        if (gameModeOverlay != null) {
            gameModeOverlay.setVisible(true);
        }
    }

    @FXML
    private void closeGameModeOverlay() {
        if (gameModeOverlay != null) {
            gameModeOverlay.setVisible(false);
        }
    }

    @FXML
    private void handleSinglePlayer(javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/GameBoard.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            GameBoardController gameBoardController = loader.getController();
            gameBoardController.initializeSinglePlayer();
            gameBoardController.setPlayerName(com.example.unofrontend.session.SessionManager.getUsername());
            Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
            boolean wasFullScreen = stage.isFullScreen();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setMaximized(true);
            if (wasFullScreen) {
                Platform.runLater(() -> stage.setFullScreen(true));
            }
            Platform.runLater(() -> {
                if (root instanceof javafx.scene.layout.Pane pane) {
                    pane.requestLayout();
                    pane.layout();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleMultiPlayer(javafx.event.ActionEvent event) {
        System.out.println("Multiplayer mode not implemented yet");
    }

    @FXML
    private void handleLeaderboard() {
        if (leaderboardOverlay != null) {
            leaderboardOverlay.setVisible(true);
            loadLeaderboardData();
        }
    }

    private void loadLeaderboardData() {
        if (periodComboBox == null || leaderboardTable == null) return;
        String period = periodComboBox.getValue();
        ObservableList<LeaderboardEntry> entries = FXCollections.observableArrayList();
        try {
            java.util.List<Leaderboard> leaderboardList;
            switch (period) {
                case "Weekly":
                    leaderboardList = apiService.getWeeklyLeaderboard();
                    break;
                case "Monthly":
                    leaderboardList = apiService.getMonthlyLeaderboard();
                    break;
                default:
                    leaderboardList = apiService.getAllTimeLeaderboard();
                    break;
            }
            for (int i = 0; i < leaderboardList.size(); i++) {
                Leaderboard dto = leaderboardList.get(i);
                entries.add(new LeaderboardEntry(
                    i + 1,
                    dto.getUsername(),
                    dto.getTotalScore() != null ? dto.getTotalScore().doubleValue() : 0.0
                ));
            }
        } catch (Exception e) {
            showError("Error loading leaderboard data: " + e.getMessage());
        }
        leaderboardTable.setItems(entries);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void closeLeaderboardOverlay() {
        if (leaderboardOverlay != null) {
            leaderboardOverlay.setVisible(false);
        }
    }

    @FXML
    private void handleLogout() {
        try {
            com.example.unofrontend.session.SessionManager.clearSession();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/login.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 