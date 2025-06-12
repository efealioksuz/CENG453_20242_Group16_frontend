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
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import com.example.unofrontend.session.SessionManager;
import com.example.unofrontend.services.WebSocketService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

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

    // Multiplayer Room Management fields
    @FXML
    private StackPane multiplayerRoomOverlay;
    @FXML
    private Button createRoomButton;
    @FXML
    private Button joinRoomButton;
    @FXML
    private Button startGameButton;
    @FXML
    private TextField roomIdInput;
    @FXML
    private Label roomIdLabel;
    @FXML
    private Label multiplayerStatusLabel;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ApiService apiService;

    @Autowired
    private WebSocketService webSocketService;

    private final ObservableList<LeaderboardEntry> leaderboardData = FXCollections.observableArrayList();
    private String currentRoomId;
    private boolean isRoomCreator = false;
    private boolean gameTransitionInProgress = false;
    private boolean hasSentPlayerReady = false;

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
            gameBoardController.setPlayerName(SessionManager.getUsername());
            Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
            
            // Get screen dimensions
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            
            // Create a new scene with screen dimensions
            Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
            stage.setScene(scene);
            
            // Position the window to fill the screen
            stage.setX(screenBounds.getMinX());
            stage.setY(screenBounds.getMinY());
            stage.setWidth(screenBounds.getWidth());
            stage.setHeight(screenBounds.getHeight());
            

            stage.setMaximized(true);
            
            Platform.runLater(() -> {
                // Double-check dimensions
                if (stage.getWidth() < screenBounds.getWidth() || stage.getHeight() < screenBounds.getHeight()) {
                    stage.setWidth(screenBounds.getWidth());
                    stage.setHeight(screenBounds.getHeight());
                    stage.setX(screenBounds.getMinX());
                    stage.setY(screenBounds.getMinY());
                }
                
                root.requestLayout();
                root.layout();
                for (javafx.scene.Node child : root.getChildrenUnmodifiable()) {
                    if (child instanceof javafx.scene.layout.Pane pane) {
                        pane.requestLayout();
                        pane.layout();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleMultiPlayer(javafx.event.ActionEvent event) {
        // Show room management overlay instead of directly starting game
        if (multiplayerRoomOverlay != null) {
            multiplayerRoomOverlay.setVisible(true);
            resetMultiplayerUI();
        }
    }

    @FXML
    private void handleCreateRoom() {
        try {
            String playerName = SessionManager.getUsername();
            if (playerName == null || playerName.trim().isEmpty()) {
                showMultiplayerError("Player name not found. Please login again.");
                return;
            }
            
            createRoomButton.setDisable(true);
            multiplayerStatusLabel.setText("Creating room...");
            multiplayerStatusLabel.setVisible(true);
            multiplayerStatusLabel.setStyle("-fx-text-fill: #3498db;");
            
            // Call REST API to create room
            new Thread(() -> {
                try {
                    Map<String, Object> response = apiService.createRoom(playerName);
                    
                    Platform.runLater(() -> {
                        Boolean success = (Boolean) response.get("success");
                        if (success != null && success) {
                            currentRoomId = (String) response.get("roomId");
                            isRoomCreator = true;
                            
                            roomIdLabel.setText("Room ID: " + currentRoomId + " (Share this with other players)");
                            roomIdLabel.setVisible(true);
                            
                            multiplayerStatusLabel.setText("Room created! Waiting for players...");
                            multiplayerStatusLabel.setStyle("-fx-text-fill: #27ae60;");
                            
                            joinRoomButton.setDisable(true);
                            roomIdInput.setDisable(true);
                            startGameButton.setVisible(isRoomCreator);
                            startGameButton.setDisable(true);
                            

                            connectToRoomWebSocket(currentRoomId);
                            
                            System.out.println("Room created with ID: " + currentRoomId);
                        } else {
                            String errorMsg = (String) response.get("message");
                            createRoomButton.setDisable(false);
                            showMultiplayerError("Failed to create room: " + errorMsg);
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        createRoomButton.setDisable(false);
                        showMultiplayerError("Failed to create room: " + e.getMessage());
                    });
                }
            }).start();
            
        } catch (Exception e) {
            createRoomButton.setDisable(false);
            showMultiplayerError("Failed to create room: " + e.getMessage());
        }
    }

    @FXML
    private void handleJoinRoom() {
        try {
            String roomId = roomIdInput.getText().trim();
            if (roomId.isEmpty()) {
                showMultiplayerError("Please enter a Room ID");
                return;
            }
            
            String playerName = SessionManager.getUsername();
            if (playerName == null || playerName.trim().isEmpty()) {
                showMultiplayerError("Player name not found. Please login again.");
                return;
            }
            
            joinRoomButton.setDisable(true);
            multiplayerStatusLabel.setText("Joining room: " + roomId + "...");
            multiplayerStatusLabel.setVisible(true);
            multiplayerStatusLabel.setStyle("-fx-text-fill: #3498db;");
            
            // Call REST API to join room
            new Thread(() -> {
                try {
                    Map<String, Object> response = apiService.joinRoom(roomId, playerName);
                    
                    Platform.runLater(() -> {
                        Boolean success = (Boolean) response.get("success");
                        if (success != null && success) {
                            currentRoomId = roomId;
                            isRoomCreator = false;
                            
                            multiplayerStatusLabel.setText("Successfully joined room: " + roomId);
                            multiplayerStatusLabel.setStyle("-fx-text-fill: #27ae60;");
                            
                            createRoomButton.setDisable(true);
                            roomIdInput.setDisable(true);
                            startGameButton.setVisible(isRoomCreator);
                            startGameButton.setDisable(true);
                            

                            connectToRoomWebSocket(currentRoomId);
                            
                            System.out.println("Successfully joined room: " + roomId);
                        } else {
                            String errorMsg = (String) response.get("message");
                            joinRoomButton.setDisable(false);
                            showMultiplayerError("Failed to join room: " + errorMsg);
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        joinRoomButton.setDisable(false);
                        showMultiplayerError("Failed to join room: " + e.getMessage());
                    });
                }
            }).start();
            
        } catch (Exception e) {
            joinRoomButton.setDisable(false);
            showMultiplayerError("Failed to join room: " + e.getMessage());
        }
    }

    @FXML
    private void handleStartMultiplayerGame() {
        try {
            if (currentRoomId == null) {
                showMultiplayerError("No room selected");
                return;
            }
            

            String roomIdToUse = currentRoomId;
            boolean roomCreator = isRoomCreator;
            
            // Stop polling but keep WebSocket connection for GameBoard
            stopRoomStatusPolling();
            
            // Close the overlay
            multiplayerRoomOverlay.setVisible(false);
            
            // Start the multiplayer game
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/gameBoard.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            
            GameBoardController gameBoardController = loader.getController();
            gameBoardController.setPlayerName(SessionManager.getUsername());
            gameBoardController.initializeMultiPlayerWithRoom(roomIdToUse, roomCreator);
            
            Stage stage = (Stage) startGameButton.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            stage.setX(screenBounds.getMinX());
            stage.setY(screenBounds.getMinY());
            stage.setWidth(screenBounds.getWidth());
            stage.setHeight(screenBounds.getHeight());
            stage.setMaximized(true);
            
        } catch (Exception e) {
            showMultiplayerError("Failed to start game: " + e.getMessage());
            e.printStackTrace();
        }
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
            SessionManager.clearSession();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/login.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            boolean wasFullScreen = stage.isFullScreen();
            boolean wasMaximized = stage.isMaximized();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            if (wasFullScreen) {
                Platform.runLater(() -> stage.setFullScreen(true));
            }
            Platform.runLater(() -> {
                root.requestLayout();
                root.layout();
                for (javafx.scene.Node child : root.getChildrenUnmodifiable()) {
                    if (child instanceof javafx.scene.layout.Pane pane) {
                        pane.requestLayout();
                        pane.layout();
                    }
                }
                Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
                stage.setX((screenBounds.getWidth() - stage.getWidth()) / 2);
                stage.setY((screenBounds.getHeight() - stage.getHeight()) / 2);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void closeMultiplayerRoomOverlay() {
        if (multiplayerRoomOverlay != null) {
            multiplayerRoomOverlay.setVisible(false);
            resetMultiplayerUI();
        }
    }

    private void resetMultiplayerUI() {
        currentRoomId = null;
        isRoomCreator = false;
        gameTransitionInProgress = false;
        hasSentPlayerReady = false;
        
        roomIdLabel.setVisible(false);
        multiplayerStatusLabel.setVisible(false);
        startGameButton.setVisible(false);
        
        createRoomButton.setDisable(false);
        joinRoomButton.setDisable(false);
        roomIdInput.setDisable(false);
        roomIdInput.clear();
    }

    private void showMultiplayerError(String message) {
        multiplayerStatusLabel.setText(message);
        multiplayerStatusLabel.setVisible(true);
        multiplayerStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
        System.out.println("Multiplayer error: " + message);
    }

    private void connectToRoomWebSocket(String roomId) {
        try {
            System.out.println("Room status polling başlatılıyor... Room: " + roomId);
            
            // WebSocket subscription for real-time room updates
            WebSocketService webSocketService = context.getBean(WebSocketService.class);
            String serverUrl = apiService.getBaseUrl() + "/uno-websocket";
            
            webSocketService.connect(serverUrl, (status) -> {
                Platform.runLater(() -> {
                    if (status.contains("Connected")) {
                        // Subscribe to room topic for real-time updates (game start, player join/leave)
                        webSocketService.subscribeToGameTopic(roomId, (message) -> {
                            Platform.runLater(() -> handleRoomUpdate(message));
                        });
                        
                        // Subscribe to error queue for per-user errors
                        webSocketService.subscribeToErrors((message) -> {
                            Platform.runLater(() -> {
                                String errorMsg = (String) message.get("message");
                                if (errorMsg != null) {
                                    System.out.println("[WebSocket ERROR] " + errorMsg);
                                }
                                @SuppressWarnings("unchecked")
                                java.util.List<String> notReadyPlayers = (java.util.List<String>) message.get("notReadyPlayers");
                                if (notReadyPlayers != null && !notReadyPlayers.isEmpty()) {
                                    System.out.println("[WebSocket ERROR] Not ready players: " + notReadyPlayers);
                                }
                            });
                        });
                        
                        // Join the room for notifications
                        String playerName = SessionManager.getUsername() != null ? 
                                          SessionManager.getUsername() : "Player1";
                        webSocketService.joinGame(roomId, playerName);

                        // Subscribe to game state queue for readiness
                        webSocketService.subscribeToGameState((message) -> {
                            System.out.println("[Lobby] Received game state message: " + message.get("type"));
                        });
                        // Only send PLAYER_READY if not already sent
                        if (!hasSentPlayerReady) {
                            Map<String, String> readyMessage = new HashMap<>();
                            readyMessage.put("gameId", roomId);
                            readyMessage.put("player", playerName);
                            readyMessage.put("type", "PLAYER_READY");
                            webSocketService.sendMessage("/app/playerReady", readyMessage);
                            System.out.println("[Lobby] PLAYER_READY sent for player: " + playerName + ", room: " + roomId);
                            hasSentPlayerReady = true;
                        }
                        updateRoomStatus("Connected to room: " + roomId + " via WebSocket");
                    } else {
                        // Fallback to polling if WebSocket fails
                        startRoomStatusPolling(roomId);
                        updateRoomStatus("Connected to room: " + roomId + " via REST polling");
                    }
                });
            });
            
        } catch (Exception e) {
            System.out.println("Room bağlantı hatası: " + e.getMessage());
            // Fallback to polling
            startRoomStatusPolling(roomId);
            updateRoomStatus("Connected to room: " + roomId + " via REST polling (WebSocket failed)");
        }
    }
    
    private void startRoomStatusPolling(String roomId) {

        Thread pollingThread = new Thread(() -> {
            while (currentRoomId != null && currentRoomId.equals(roomId)) {
                try {
                    Map<String, Object> roomStatus = apiService.getRoomStatus(roomId);
                    
                    Platform.runLater(() -> {
                        if (roomStatus != null && (Boolean) roomStatus.get("success")) {
                            Integer playerCount = (Integer) roomStatus.get("playerCount");
                            @SuppressWarnings("unchecked")
                            List<String> players = (List<String>) roomStatus.get("players");
                            
                            if (playerCount != null && players != null) {
                                updateRoomStatus("Players in room (" + playerCount + "/4): " + 
                                               String.join(", ", players));
                                

                                if (playerCount >= 2) {
                                    startGameButton.setDisable(false);
                                } else {
                                    startGameButton.setDisable(true);
                                }
                            }
                        }
                    });
                    
                    Thread.sleep(3000);
                } catch (Exception e) {
                    System.out.println("Room status polling hatası: " + e.getMessage());
                    break;
                }
            }
        });
        pollingThread.setDaemon(true);
        pollingThread.start();
    }
    
    private void handleRoomUpdate(Map<String, Object> message) {
        try {
            String type = (String) message.get("type");
            System.out.println("Room update alındı: " + type);
            
            // Check for readyStates in every message
            @SuppressWarnings("unchecked")
            Map<String, Boolean> readyStates = (Map<String, Boolean>) message.get("readyStates");
            if (readyStates != null) {
                boolean allReady = !readyStates.isEmpty() && readyStates.values().stream().allMatch(Boolean::booleanValue);
                if (isRoomCreator) {
                    startGameButton.setDisable(!allReady);
                    startGameButton.setVisible(true);
                    if (allReady) {
                        updateRoomStatus("All players are ready! You can start the game.");
                    } else {
                        updateRoomStatus("Waiting for all players to be ready...");
                    }
                }
            }

            // Log notReadyPlayers if present
            @SuppressWarnings("unchecked")
            java.util.List<String> notReadyPlayers = (java.util.List<String>) message.get("notReadyPlayers");
            if (notReadyPlayers != null && !notReadyPlayers.isEmpty()) {
                System.out.println("[Lobby] Not ready players: " + notReadyPlayers);
            }

            switch (type) {
                case "PLAYER_CONNECTED":
                    // Handle player connection to room - just update status
                    String connectedPlayer = (String) message.get("player");
                    if (connectedPlayer != null) {
                        updateRoomStatus(connectedPlayer + " connected to room");
                    }
                    break;
                    
                case "PLAYER_JOINED":
                    String joinedPlayer = (String) message.get("player");
                    Integer playerCount = (Integer) message.get("playerCount");
                    updateRoomStatus(joinedPlayer + " joined! Players: " + playerCount + "/4");
                    

                    if (playerCount != null) {
                        if (playerCount >= 2) {
                            if (isRoomCreator) {
                                startGameButton.setDisable(false);
                                startGameButton.setVisible(true);
                                updateRoomStatus("Ready to start! Click 'Start Game'");
                            } else {
                                startGameButton.setVisible(false);
                            }
                        } else {
                            startGameButton.setDisable(true);
                            if (isRoomCreator) {
                                startGameButton.setVisible(true);
                            } else {
                                startGameButton.setVisible(false);
                            }
                        }
                    }
                    break;
                    
                case "PLAYER_LEFT":
                    String leftPlayer = (String) message.get("player");
                    Integer remainingCount = (Integer) message.get("playerCount");
                    updateRoomStatus(leftPlayer + " left. Players: " + remainingCount + "/4");
                    

                    if (remainingCount < 2) {
                        startGameButton.setDisable(true);
                        updateRoomStatus("Waiting for more players... (" + remainingCount + "/4)");
                    }
                    break;
                    
                case "GAME_STARTED":
                    // Check if transition is already in progress
                    if (gameTransitionInProgress) {
                        System.out.println("Game transition already in progress, ignoring duplicate GAME_STARTED message");
                        return;
                    }
                    
                    // Another player started the game - automatically join the game
                    System.out.println("Game started by another player - automatically joining...");
                    updateRoomStatus("Game started! Joining game...");
                    
                    // Set flag to prevent duplicate transitions
                    gameTransitionInProgress = true;
                    
                    Platform.runLater(() -> {
                        try {
                            // Validate that we still have the room management UI visible
                            if (multiplayerRoomOverlay == null || !multiplayerRoomOverlay.isVisible()) {
                                System.out.println("Room overlay not visible, skipping auto-join");
                                gameTransitionInProgress = false;
                                return;
                            }
                            
                            // Validate room ID
                            if (currentRoomId == null || currentRoomId.trim().isEmpty()) {
                                System.out.println("No current room ID, cannot auto-join game");
                                gameTransitionInProgress = false;
                                return;
                            }
                            
                            // Get stage from a reliable source - try multiple UI elements
                            Stage stage = null;
                            
                            // Try to get stage from multiple UI elements
                            if (multiplayerRoomOverlay.getScene() != null && multiplayerRoomOverlay.getScene().getWindow() instanceof Stage) {
                                stage = (Stage) multiplayerRoomOverlay.getScene().getWindow();
                            } else if (startGameButton.getScene() != null && startGameButton.getScene().getWindow() instanceof Stage) {
                                stage = (Stage) startGameButton.getScene().getWindow();
                            } else if (createRoomButton.getScene() != null && createRoomButton.getScene().getWindow() instanceof Stage) {
                                stage = (Stage) createRoomButton.getScene().getWindow();
                            }
                            
                            if (stage == null) {
                                System.out.println("Cannot get stage reference - stage is null");
                                showMultiplayerError("Failed to join started game: Cannot access window");
                                gameTransitionInProgress = false;
                                return;
                            }
                            
                            // Use the same logic as handleStartMultiplayerGame
                            String roomIdToUse = currentRoomId;
                            boolean roomCreator = isRoomCreator;
                            
                            // Stop polling
                            stopRoomStatusPolling();
                            
                            // Close the overlay
                            multiplayerRoomOverlay.setVisible(false);
                            
                            // Start the multiplayer game
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/gameBoard.fxml"));
                            loader.setControllerFactory(context::getBean);
                            Parent root = loader.load();
                            
                            GameBoardController gameBoardController = loader.getController();
                            gameBoardController.setPlayerName(SessionManager.getUsername());
                            gameBoardController.initializeMultiPlayerWithRoom(roomIdToUse, roomCreator);
                            
                            Scene scene = new Scene(root);
                            stage.setScene(scene);
                            
                            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
                            stage.setX(screenBounds.getMinX());
                            stage.setY(screenBounds.getMinY());
                            stage.setWidth(screenBounds.getWidth());
                            stage.setHeight(screenBounds.getHeight());
                            stage.setMaximized(true);
                            
                            System.out.println("Successfully transitioned to multiplayer game");
                            
                        } catch (Exception e) {
                            System.out.println("Failed to auto-join started game: " + e.getMessage());
                            e.printStackTrace();
                            showMultiplayerError("Failed to join started game: " + e.getMessage());
                        } finally {
                            gameTransitionInProgress = false;
                        }
                    });
                    break;
                    
                default:
                    System.out.println("Unknown room message type: " + type);
            }
        } catch (Exception e) {
            System.out.println("Room message error: " + e.getMessage());
            e.printStackTrace();
            gameTransitionInProgress = false;
        }
    }
    
    private void updateRoomStatus(String message) {
        if (multiplayerStatusLabel != null) {
            multiplayerStatusLabel.setText(message);
            multiplayerStatusLabel.setVisible(true);
            multiplayerStatusLabel.setStyle("-fx-text-fill: #3498db;");
        }
    }

    private void stopRoomStatusPolling() {
        System.out.println("Stopping room status polling...");
        currentRoomId = null;
        hasSentPlayerReady = false;
        updateRoomStatus("Game starting... Polling stopped.");
    }
} 