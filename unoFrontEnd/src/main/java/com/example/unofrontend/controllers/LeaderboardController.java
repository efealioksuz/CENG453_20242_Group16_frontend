package com.example.unofrontend.controllers;

import com.example.unofrontend.models.LeaderboardEntry;
import com.example.unofrontend.models.Leaderboard;
import com.example.unofrontend.services.ApiService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import javafx.scene.control.TableView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class LeaderboardController {
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
    @FXML
    private Button backButton;

    @Autowired
    private ApiService apiService;

    @Autowired
    private ApplicationContext context;

    private final ObservableList<LeaderboardEntry> leaderboardData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        leaderboardTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        periodComboBox.setItems(FXCollections.observableArrayList("All Time", "Monthly", "Weekly"));
        rankColumn.setCellValueFactory(new PropertyValueFactory<>("rank"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        scoreColumn.setCellValueFactory(new PropertyValueFactory<>("score"));
        periodComboBox.setValue("All Time");
        periodComboBox.setOnAction(e -> loadLeaderboardData());
        loadLeaderboardData();
    }

    private void loadLeaderboardData() {
        String period = periodComboBox.getValue();
        List<LeaderboardEntry> entries = new ArrayList<>();

        
        try {
            List<Leaderboard> leaderboardData;
            switch (period) {
                case "Weekly":
                    leaderboardData = apiService.getWeeklyLeaderboard();
                    break;
                case "Monthly":
                    leaderboardData = apiService.getMonthlyLeaderboard();
                    break;
                default:
                    leaderboardData = apiService.getAllTimeLeaderboard();
                    break;
            }

          
            for (int i = 0; i < leaderboardData.size(); i++) {
                Leaderboard dto = leaderboardData.get(i);
                entries.add(new LeaderboardEntry(
                    i + 1,
                    dto.getUsername(),
                    dto.getTotalScore().doubleValue()
                ));
            }
        } catch (Exception e) {
            showError("Error loading leaderboard data: " + e.getMessage());
        }

      
        leaderboardTable.setItems(FXCollections.observableArrayList(entries));
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/menu.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            
            Stage stage = (Stage) backButton.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 