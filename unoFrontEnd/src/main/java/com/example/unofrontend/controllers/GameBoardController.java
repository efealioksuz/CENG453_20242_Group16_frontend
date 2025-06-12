package com.example.unofrontend.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.example.unofrontend.models.CardData;
import com.example.unofrontend.models.GameState;
import com.example.unofrontend.models.AIPlayer;
import com.example.unofrontend.controllers.CardController;
import com.example.unofrontend.services.ApiService;
import com.example.unofrontend.services.WebSocketService;
import com.example.unofrontend.session.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
import org.springframework.stereotype.Component;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.shape.Rectangle;
import javafx.scene.input.MouseEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Component
public class GameBoardController {
    @FXML
    private Button backToMainMenu;
    @FXML
    private HBox playerHandBoxTop;
    @FXML
    private HBox playerHandBoxBottom;
    @FXML
    private VBox opponentHandBoxLeft;
    @FXML
    private VBox opponentHandBoxRight;
    @FXML
    private HBox centerPileBox;
    @FXML
    private Label currentColorLabel;
    @FXML
    private HBox colorSelectionBox;
    @FXML
    private Button unoButton;
    @FXML
    private DirectionIndicatorController directionIndicatorController;
    @FXML
    private StackPane drawPileBox;
    @FXML
    private Rectangle currentColorRect;
    @FXML
    private StackPane gameOverOverlay;
    @FXML
    private Label gameOverMessage;
    @FXML
    private Label playerUnoIndicator;
    @FXML
    private Label player2UnoIndicator;
    @FXML
    private Label player3UnoIndicator;
    @FXML
    private Label player4UnoIndicator;
    @FXML
    private ImageView topPlayerArrow;
    @FXML
    private ImageView bottomPlayerArrow;
    @FXML
    private ImageView leftPlayerArrow;
    @FXML
    private ImageView rightPlayerArrow;
    @FXML
    private Label playerNameLabel;
    @FXML
    private Label player2Label;
    @FXML
    private Label player3Label;
    @FXML
    private Label player4Label;
    @FXML
    private ImageView directionIndicator;
    @FXML
    private VBox cheatButtonsBox;
    @FXML
    private Button skipCheatButton;
    @FXML
    private Button reverseCheatButton;
    @FXML
    private Button drawTwoCheatButton;
    @FXML
    private Button wildCheatButton;
    @FXML
    private Button wildDrawFourCheatButton;
    @FXML
    private HBox playerNameBox;

    private static final String[] COLORS = {"red", "yellow", "green", "blue"};
    private static final String[] VALUES = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "Skip", "Reverse", "Draw Two"};
    private static final String[] WILD_VALUES = {"Wild", "Wild Draw Four"};
    private static final Image UNO_LOGO;

    static {
        try {
            UNO_LOGO = new Image(GameBoardController.class.getResourceAsStream("/images/UNO_Logo.svg.png"));
        } catch (Exception e) {
            System.err.println("Failed to load UNO logo: " + e.getMessage());
            throw new RuntimeException("Failed to load UNO logo", e);
        }
    }

    private GameState gameState;
    private AIPlayer aiPlayer;
    private boolean isPlayerTurn;
    private String playerName = "Player 1";
    private boolean playerWonGame = false;
    @FXML
    private ImageView drawPileLogo;
    private CardData pendingWildCard = null;
    private final ApplicationContext context;
    private Image clockwiseImage;
    private Image counterClockwiseImage;
    @Autowired
    private ApiService apiService;
    
    // WebSocket değişkenleri
    private String gameRoomId;
    private boolean isWebSocketConnected = false;
    private boolean isRoomCreator = false;
    private boolean isMultiplayerGame = false;
    private int pendingMultiplayerCardIndex = -1;
    @FXML
    private Label webSocketStatusLabel;

    public GameBoardController(ApplicationContext context) {
        this.context = context;
    }

    @FXML
    public void initialize() {
        System.out.println("Game board initialized");
        try {
            clockwiseImage = new Image(getClass().getResourceAsStream("/images/rotate-right.png"));
            counterClockwiseImage = new Image(getClass().getResourceAsStream("/images/rotate-left.png"));
            
            // WebSocket durum etiketi oluştur
            webSocketStatusLabel = new Label("WebSocket: Disconnected");
            webSocketStatusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            if (playerNameBox != null) {
                playerNameBox.getChildren().add(webSocketStatusLabel);
            }
            
            System.out.println("Direction images loaded: " + 
                             "clockwise=" + (clockwiseImage != null && !clockwiseImage.isError()) + 
                             ", counter-clockwise=" + (counterClockwiseImage != null && !counterClockwiseImage.isError()));

            if (clockwiseImage == null || counterClockwiseImage == null || 
                clockwiseImage.isError() || counterClockwiseImage.isError()) {
                System.err.println("Failed to load direction images");
                if (clockwiseImage != null && clockwiseImage.isError()) {
                    System.err.println("Clockwise image error: " + clockwiseImage.getException());
                }
                if (counterClockwiseImage != null && counterClockwiseImage.isError()) {
                    System.err.println("Counter-clockwise image error: " + counterClockwiseImage.getException());
                }
            }

            if (drawPileLogo != null && UNO_LOGO != null) {
                drawPileLogo.setImage(UNO_LOGO);
                drawPileLogo.setFitWidth(84);
                drawPileLogo.setFitHeight(126);
                drawPileLogo.setPreserveRatio(true);
                drawPileLogo.setSmooth(true);
            } else {
                System.err.println("drawPileLogo or UNO_LOGO is null");
            }
            
            drawPileBox.setOnMouseClicked(event -> handleDrawPileClick());
            loadArrowImages();

            // Initialize direction indicator
            if (directionIndicator != null) {
                directionIndicator.setImage(clockwiseImage);
                System.out.println("Direction indicator initialized");
            } else {
                System.err.println("Direction indicator is NULL");
            }
            
            // Initialize cheat buttons
            updateCheatButtonsVisibility();

           
            if (backToMainMenu != null) {
                backToMainMenu.getStyleClass().add("back-to-menu-button");
                backToMainMenu.setStyle("-fx-font-size: 16px; -fx-text-fill: white; -fx-background-color: #ff4d4d; -fx-font-weight: bold; -fx-padding: 8 15 8 15;");
            }

            if (playerNameBox != null && playerHandBoxBottom != null) {
                playerNameBox.prefWidthProperty().bind(playerHandBoxBottom.widthProperty());
            }
        } catch (Exception e) {
            System.err.println("Error during initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void initializeSinglePlayer() {
        gameState = new GameState();
        aiPlayer = new AIPlayer();
        isPlayerTurn = true;
        
        playerUnoIndicator.setVisible(false);
        player2UnoIndicator.setVisible(false);
        player3UnoIndicator.setVisible(false);
        player4UnoIndicator.setVisible(false);
        
        gameState.startGame();
        
        if (playerNameLabel != null) {
            playerNameLabel.setText(playerName);
        }
        
        addCardsToHand(gameState.getPlayerHand(0), playerHandBoxBottom); 
        addCardsToHand(gameState.getPlayerHand(2), playerHandBoxTop);
        addCardsToHand(gameState.getPlayerHand(1), opponentHandBoxLeft);
        addCardsToHand(gameState.getPlayerHand(3), opponentHandBoxRight);
        
        CardData topCard = gameState.getTopCard();
        if (topCard != null) {
            addCardToCenterPile(topCard);
        }
        
        StackPane drawPile = createClosedCard();
        drawPileBox.getChildren().add(drawPile);
        
        updateCurrentColorLabel();
        updateDirectionIndicator();
        
        gameState.setCurrentPlayerIndex(0);
        isPlayerTurn = true;
        forceUpdateArrows();
    }

    public void initializeMultiPlayer() {

        initializeSinglePlayer();
        

        if (webSocketStatusLabel != null) {
            webSocketStatusLabel.setVisible(true);
        }
        

        Platform.runLater(() -> {
            try {
                System.out.println("WebSocket multiplayer modu başlatılıyor...");
                connectToWebSocket();
            } catch (Exception e) {
                System.out.println("WebSocket başlatma hatası: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void initializeMultiPlayerWithRoom(String roomId, boolean isCreator) {
        // Set multiplayer flags FIRST
        this.gameRoomId = roomId;
        this.isRoomCreator = isCreator;
        this.isMultiplayerGame = true;
        
        System.out.println("Initializing multiplayer game with room: " + roomId + ", isCreator: " + isCreator);
        


        isPlayerTurn = false; // Wait for backend to determine turn
        
        playerUnoIndicator.setVisible(false);
        player2UnoIndicator.setVisible(false);
        player3UnoIndicator.setVisible(false);
        player4UnoIndicator.setVisible(false);
        
        // Hide player name labels initially - will be shown with actual names when game starts
        if (player2Label != null) player2Label.setVisible(false);
        if (player3Label != null) player3Label.setVisible(false);
        if (player4Label != null) player4Label.setVisible(false);
        
        // Do NOT start local game
        // gameState.startGame(); // REMOVED
        
        if (playerNameLabel != null) {
            playerNameLabel.setText(playerName);
        }
        
        // Clear all hands - will be populated by backend
        playerHandBoxBottom.getChildren().clear();
        opponentHandBoxLeft.getChildren().clear();
        playerHandBoxTop.getChildren().clear(); 
        opponentHandBoxRight.getChildren().clear();
        
        // Clear center pile - will be set by backend
        centerPileBox.getChildren().clear();
        drawPileBox.getChildren().clear();
        
        // Add draw pile placeholder
        StackPane drawPile = createClosedCard();
        drawPileBox.getChildren().add(drawPile);
        
        // Reset indicators - will be updated by backend
        if (currentColorRect != null) {
            currentColorRect.getStyleClass().removeAll("red-square", "blue-square", "green-square", "yellow-square");
        }
        
        // Hide arrows until backend determines turn order
        hideAllArrows();
        
        // Show WebSocket status
        if (webSocketStatusLabel != null) {
            webSocketStatusLabel.setVisible(true);
            updateWebSocketStatus("Connecting to multiplayer game...");
        }
        
        // Check if WebSocket is already connected and has the right subscription
        Platform.runLater(() -> {
            try {
                System.out.println("Setting up WebSocket for multiplayer room: " + roomId);
                
                WebSocketService webSocketService = context.getBean(WebSocketService.class);
                
                if (webSocketService.isConnected()) {
                    System.out.println("WebSocket already connected, setting up game subscription");
                    updateWebSocketStatus("WebSocket: Connected");
                    isWebSocketConnected = true;
                    
                    // Set up game state subscription for this specific game
                    setupGameStateSubscription(webSocketService);
                    
                    // Only the creator should send the start game request
                    if (isRoomCreator) {
                        Timeline delayedStartRequest = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                            sendStartGameRequest();
                            System.out.println("Start game request sent for room: " + roomId);
                        }));
                        delayedStartRequest.play();
                    }
                } else {
                    System.out.println("WebSocket not connected, creating new connection");
                    connectToWebSocketWithRoom(roomId);
                }
                
            } catch (Exception e) {
                System.out.println("Error setting up multiplayer WebSocket: " + e.getMessage());
                e.printStackTrace();
                updateWebSocketStatus("WebSocket: Setup failed");
            }
        });
    }

    private void setupGameStateSubscription(WebSocketService webSocketService) {
        // Subscribe to individual game state (with playerIndex)
        webSocketService.subscribeToGameState((message) -> {
            System.out.println("[GameBoard] Received game state message: " + message);
            Platform.runLater(() -> handleWebSocketMessage(message));
        });
        
        System.out.println("Game state subscription set up for room: " + gameRoomId);

        // Send GET_GAME_STATE message after subscription is set up
        try {
            Map<String, String> getStateMessage = new HashMap<>();
            getStateMessage.put("gameId", gameRoomId);
            getStateMessage.put("player", playerName);
            getStateMessage.put("type", "GET_GAME_STATE");
            webSocketService.sendMessage("/app/getGameState", getStateMessage);
            System.out.println("GET_GAME_STATE message sent for player: " + playerName + ", room: " + gameRoomId);
        } catch (Exception e) {
            System.err.println("Failed to send GET_GAME_STATE message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addPlaceholderCards(Pane handBox, int count) {
        handBox.getChildren().clear();
        for (int i = 0; i < count; i++) {
            StackPane placeholderCard = createClosedCard();
            placeholderCard.setOpacity(1.0); // Remove transparency
            handBox.getChildren().add(placeholderCard);
        }
    }

    private void updateCurrentColorLabel() {
        String currentColor = gameState.getCurrentColor();
        currentColorRect.getStyleClass().removeAll("red-square", "blue-square", "green-square", "yellow-square");
        currentColorRect.getStyleClass().add(currentColor.toLowerCase() + "-square");
    }

    private void handleCardClick(CardData card, int cardIndex) {
        if (!isPlayerTurn) return;
        
        if (isMultiplayerGame) {
            // In multiplayer mode, send card play to backend
            sendCardPlayToServer(card, cardIndex);
            return;
        }
        
        // Single player mode logic
        if (gameState.getDrawStack() > 0) {
            CardData topCard = gameState.getTopCard();
            if (topCard.value.equals("Draw Two") && !card.value.equals("Draw Two")) {
                return;
            }
        }
        
        if (gameState.canPlayCard(card)) {
            System.out.println("Playing card: " + card.value + " of " + card.color + " at index: " + cardIndex);
            
            if (card.value.equals("Wild") || card.value.equals("Wild Draw Four")) {
                colorSelectionBox.setVisible(true);
                pendingWildCard = card;
                drawPileBox.getProperties().put("pendingCardIndex", cardIndex);
                return;
            }
            
            CardData topCard = gameState.getTopCard();
            String colorToUse = card.value.equals(topCard.value) ? gameState.getCurrentColor() : card.color;
            playCard(card, colorToUse, cardIndex);
        }
    }

    private void handleDrawPileClick() {
        if (!isPlayerTurn) return;

        if (isMultiplayerGame) {
            // In multiplayer mode, send draw request to backend
            sendDrawRequestToServer();
            return;
        }
        
        // Single player mode logic
        if (gameState.getDrawStack() > 0) {
            return;
        }
        
        boolean hasPlayableCard = false;
        List<CardData> playerHand = gameState.getPlayerHand(0);
        for (CardData card : playerHand) {
            if (gameState.canPlayCard(card)) {
                hasPlayableCard = true;
                break;
            }
        }
        
        if (!hasPlayableCard) {
            CardData drawnCard = gameState.drawCard();
            if (drawnCard != null) {
                playerHand.add(drawnCard);
                refreshPlayerHand();
                
                // Check if the drawn card can be played
                if (drawnCard.value.equals("Wild") || 
                    drawnCard.value.equals("Wild Draw Four") || 
                    drawnCard.color.equals(gameState.getCurrentColor()) || 
                    drawnCard.value.equals(gameState.getTopCard().value)) {
                    
                    // Let the player decide if they want to play the drawn card
                    System.out.println("Drew a playable card: " + drawnCard.value + " of " + drawnCard.color);
                } else {
                    // If card cannot be played, move to next player
                    gameState.moveToNextPlayer();
                    isPlayerTurn = false;
                    forceUpdateArrows();
                    playAITurns();
                }
            }
        }
    }

    private void sendCardPlayToServer(CardData card, int cardIndex) {
        try {
            WebSocketService webSocketService = context.getBean(WebSocketService.class);
            
            Map<String, String> cardMessage = new HashMap<>();
            cardMessage.put("gameId", gameRoomId);
            cardMessage.put("player", playerName);
            cardMessage.put("card", card.color + "_" + card.value);
            cardMessage.put("color", card.color);
            
            // Handle wild cards
            if (card.value.equals("Wild") || card.value.equals("Wild Draw Four")) {
                // Show color selection for wild cards
                colorSelectionBox.setVisible(true);
                pendingWildCard = card;
                pendingMultiplayerCardIndex = cardIndex;
                return;
            }
            
            webSocketService.sendMessage("/app/playCard", cardMessage);
            System.out.println("Card play sent to server: " + card.color + "_" + card.value);
            
        } catch (Exception e) {
            System.err.println("Failed to send card play to server: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void sendDrawRequestToServer() {
        try {
            WebSocketService webSocketService = context.getBean(WebSocketService.class);
            
            Map<String, String> drawMessage = new HashMap<>();
            drawMessage.put("gameId", gameRoomId);
            drawMessage.put("player", playerName);
            drawMessage.put("drawCount", "1");
            
            webSocketService.sendMessage("/app/drawCard", drawMessage);
            System.out.println("Draw request sent to server");
            
        } catch (Exception e) {
            System.err.println("Failed to send draw request to server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendStartGameRequest() {
        try {
            WebSocketService webSocketService = context.getBean(WebSocketService.class);
            
            Map<String, String> startMessage = new HashMap<>();
            startMessage.put("gameId", gameRoomId);
            startMessage.put("player", playerName);
            
            webSocketService.sendMessage("/app/startGame", startMessage);
            System.out.println("Start game request sent to server for room: " + gameRoomId);
            
        } catch (Exception e) {
            System.err.println("Failed to send start game request to server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void refreshPlayerHand() {
        List<CardData> playerHand = gameState.getPlayerHand(0);
        playerHandBoxBottom.getChildren().clear();
        addCardsToHand(playerHand, playerHandBoxBottom);
    }

    private void playAITurns() {

        int nextPlayerIndex = gameState.getCurrentPlayerIndex();
        
       
        
        playNextAITurn(nextPlayerIndex);
    }

    private void playNextAITurn(int playerIndex) {
        if (playerIndex >= 4 || playerIndex == 0) {
            isPlayerTurn = true;
            System.out.println("AI turns completed, returning control to player");
            
            if (gameState.getDrawStack() > 0) {
                checkAndHandleForcedDraws();
            }
            forceUpdateArrows();
            return;
        }

        String playerName;
        switch (playerIndex) {
            case 1: playerName = "Player 2"; break;
            case 2: playerName = "Player 3"; break;
            case 3: playerName = "Player 4"; break;
            default: playerName = "Unknown Player";
        }

        String logPlayerName = "Player " + (playerIndex + 1);
        
        forceUpdateArrows();

        Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1.5), e -> {
            List<CardData> aiHand = gameState.getPlayerHand(playerIndex);
        
            if (gameState.getDrawStack() > 0) {
                CardData drawCard = aiPlayer.chooseDrawCardToPlay(aiHand, gameState);
                if (drawCard != null) {
                    String chosenColor = null;
                    if (drawCard.value.equals("Wild") || drawCard.value.equals("Wild Draw Four")) {
                        chosenColor = aiPlayer.chooseColor(aiHand);
                        System.out.println(logPlayerName + " chose color: " + chosenColor);
                    } else {
                        chosenColor = drawCard.color;
                    }
                    
                    int cardIndex = -1;
                    for (int i = 0; i < aiHand.size(); i++) {
                        CardData card = aiHand.get(i);
                        if (card.value.equals(drawCard.value) && card.color.equals(drawCard.color)) {
                            cardIndex = i;
                            break;
                        }
                    }
                    
                    if (cardIndex != -1) {
                        System.out.println(logPlayerName + " played: " + drawCard.value + " of " + drawCard.color);
                        aiHand.remove(cardIndex);
                        CardData cardToPlay = new CardData(drawCard.value, drawCard.color);
                        Pane handBox = (playerIndex == 1) ? opponentHandBoxLeft : 
                                     (playerIndex == 2) ? playerHandBoxTop : opponentHandBoxRight;
                        handBox.getChildren().clear();
                        addCardsToHand(aiHand, handBox);
                        gameState.playCard(playerIndex, cardToPlay, chosenColor);
                        addCardToCenterPile(cardToPlay);
                        updateCurrentColorLabel();
                        updateDirectionIndicator();

                        if (aiHand.isEmpty()) {
                            playerWonGame = false;
                            showGameOver("Game Over! " + logPlayerName + " Won!");
                            return;
                        }
                    }
                    
                    playNextAITurn(gameState.getCurrentPlayerIndex());
                } else {
                    gameState.drawCards(playerIndex);
                    
                    Pane handBox = (playerIndex == 1) ? opponentHandBoxLeft : 
                                 (playerIndex == 2) ? playerHandBoxTop : opponentHandBoxRight;
                    handBox.getChildren().clear();
                    addCardsToHand(aiHand, handBox);
                    playNextAITurn(gameState.getCurrentPlayerIndex());
                }
                return;
            }

            CardData chosenCard = aiPlayer.chooseCardToPlay(aiHand, gameState);
            
            if (chosenCard != null) {
                String chosenColor = null;
                if (chosenCard.value.equals("Wild") || chosenCard.value.equals("Wild Draw Four")) {
                    chosenColor = aiPlayer.chooseColor(aiHand);
                    System.out.println(logPlayerName + " chose color: " + chosenColor);
                } else {
                    chosenColor = chosenCard.color;
                }
                
                int cardIndex = -1;
                for (int i = 0; i < aiHand.size(); i++) {
                    CardData card = aiHand.get(i);
                    if (card.value.equals(chosenCard.value) && card.color.equals(chosenCard.color)) {
                        cardIndex = i;
                        break;
                    }
                }
                
                if (cardIndex != -1) {
                    System.out.println(logPlayerName + " played: " + chosenCard.value + " of " + chosenCard.color);
                    aiHand.remove(cardIndex);
                    CardData cardToPlay = new CardData(chosenCard.value, chosenCard.color);
                    Pane handBox = (playerIndex == 1) ? opponentHandBoxLeft : 
                                 (playerIndex == 2) ? playerHandBoxTop : opponentHandBoxRight;
                    handBox.getChildren().clear();
                    addCardsToHand(aiHand, handBox);
                    gameState.playCard(playerIndex, cardToPlay, chosenColor);
                    addCardToCenterPile(cardToPlay);
                    updateCurrentColorLabel();
                    updateDirectionIndicator();

                    if (aiHand.isEmpty()) {
                        playerWonGame = false;
                        showGameOver("Game Over! " + logPlayerName + " Won!");
                        return;
                    }
                }

                playNextAITurn(gameState.getCurrentPlayerIndex());
            } else {
                System.out.println(logPlayerName + " has no playable cards, drawing one card");
                CardData drawnCard = gameState.drawCard();
                if (drawnCard != null) {
                    System.out.println(logPlayerName + " drew: " + drawnCard.value + " of " + drawnCard.color);
                    aiHand.add(drawnCard);
                    
                    Pane handBox = (playerIndex == 1) ? opponentHandBoxLeft : 
                                 (playerIndex == 2) ? playerHandBoxTop : opponentHandBoxRight;
                    handBox.getChildren().clear();
                    addCardsToHand(aiHand, handBox);

                    CardData cardToPlay = aiPlayer.chooseCardAfterDraw(drawnCard, gameState);
                    if (cardToPlay != null) {
                        Timeline showDrawnCardTimeline = new Timeline();
                        showDrawnCardTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1.5), showEvent -> {
                            String chosenColor = null;
                            if (cardToPlay.value.equals("Wild") || cardToPlay.value.equals("Wild Draw Four")) {
                                chosenColor = aiPlayer.chooseColor(aiHand);
                                System.out.println(logPlayerName + " chose color: " + chosenColor);
                            } else {
                                chosenColor = cardToPlay.color;
                            }
                            
                            int cardIndex = -1;
                            for (int i = 0; i < aiHand.size(); i++) {
                                CardData card = aiHand.get(i);
                                if (card.value.equals(cardToPlay.value) && card.color.equals(cardToPlay.color)) {
                                    cardIndex = i;
                                    break;
                                }
                            }
                            
                            if (cardIndex != -1) {
                                System.out.println(logPlayerName + " played drawn card: " + cardToPlay.value + " of " + cardToPlay.color);
                                aiHand.remove(cardIndex);
                                handBox.getChildren().clear();
                                addCardsToHand(aiHand, handBox);
                                gameState.playCard(playerIndex, cardToPlay, chosenColor);
                                addCardToCenterPile(cardToPlay);
                                updateCurrentColorLabel();
                                updateDirectionIndicator();

                                if (aiHand.isEmpty()) {
                                    playerWonGame = false;
                                    showGameOver("Game Over! " + logPlayerName + " Won!");
                                    return;
                                }
                            }
                            
                            playNextAITurn(gameState.getCurrentPlayerIndex());
                        }));
                        showDrawnCardTimeline.play();
                    } else {
                        System.out.println(logPlayerName + " could not play the drawn card");
                        gameState.moveToNextPlayer();
                        playNextAITurn(gameState.getCurrentPlayerIndex());
                    }
                }
            }
        }));
        
        timeline.play();
    }

    private void checkAndHandleForcedDraws() {
        if (!isPlayerTurn) return;
        
        if (gameState.getDrawStack() > 0) {
            CardData topCard = gameState.getTopCard();
            
            if (topCard.value.equals("Wild Draw Four")) {
                System.out.println("Automatically drawing 4 cards due to Wild Draw Four");
                gameState.drawCards(0);
                playerHandBoxBottom.getChildren().clear();
                addCardsToHand(gameState.getPlayerHand(0), playerHandBoxBottom);
                isPlayerTurn = false;
                forceUpdateArrows();
                playAITurns();
                return;
            }
            
            if (topCard.value.equals("Draw Two")) {
                boolean hasDrawTwoCard = false;
                for (CardData handCard : gameState.getPlayerHand(0)) {
                    if (handCard.value.equals("Draw Two")) {
                        hasDrawTwoCard = true;
                        break;
                    }
                }
                
                if (!hasDrawTwoCard) {
                    System.out.println("Automatically drawing cards due to Draw Two");
                    gameState.drawCards(0);
                    playerHandBoxBottom.getChildren().clear();
                    addCardsToHand(gameState.getPlayerHand(0), playerHandBoxBottom);
                    isPlayerTurn = false;
                    forceUpdateArrows();
                    playAITurns();
                }
            }
        }
    }

    private void showGameOver(String message) {
        // Make sure the message is visible by ensuring proper formatting
        gameOverMessage.setText(message);
        gameOverMessage.setWrapText(true);
        gameOverMessage.setMaxWidth(500);
        

        String username = SessionManager.getUsername();
        if (username != null && !username.isEmpty()) {

            int scoreChange = playerWonGame ? 1 : -1;
            String result = apiService.updateDailyScore(scoreChange);
            if (!"success".equals(result)) {
                System.err.println("Failed to update daily score: " + result);
            } else {
                System.out.println("Updated score for " + username + ": " + scoreChange);
            }
        }
        
        // Show the overlay
        gameOverOverlay.setVisible(true);
        gameOverOverlay.setMouseTransparent(false);
        
        // Make sure the overlay is on top
        gameOverOverlay.toFront();
        
        // Reset the flag for next game
        playerWonGame = false;
    }

    private void addCardsToHand(List<CardData> cards, Pane handBox) {
        System.out.println("Adding " + cards.size() + " cards to hand");
        handBox.getChildren().clear();
        
        for (int i = 0; i < cards.size(); i++) {
            CardData card = cards.get(i);
            StackPane cardPane = createCardPane(card, i);
            
            if (handBox == playerHandBoxBottom) {
                cardPane.getStyleClass().add("bottom-deck-card");
            }
            
            handBox.getChildren().add(cardPane);
            System.out.println("Added card: " + card.value + " of " + card.color + " at index " + i);
        }

        System.out.println("Hand now has " + handBox.getChildren().size() + " cards");

        int playerIndex = -1;
        if (handBox == playerHandBoxBottom) playerIndex = 0;
        else if (handBox == opponentHandBoxLeft) playerIndex = 1;
        else if (handBox == playerHandBoxTop) playerIndex = 2;
        else if (handBox == opponentHandBoxRight) playerIndex = 3;
        
        if (playerIndex != -1) {
            // Only use direct mapping in single player mode
            if (!isMultiplayerGame) {
                updateUnoIndicator(playerIndex, cards.size());
            }
            // In multiplayer, UNO indicators are updated elsewhere with the mapping function
        }
    }

    private StackPane createCardPane(CardData card, int index) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/Card.fxml"));
            StackPane cardPane = loader.load();
            CardController cardController = loader.getController();
            cardController.setCard(card.value, card.color);
            
            cardPane.setUserData(cardController);
            
            cardPane.getProperties().put("cardIndex", index);
            
            cardPane.setOnMouseClicked(event -> {
                if (cardPane.getParent() == playerHandBoxBottom) {
                    handleCardClick(card, index);
                }
            });
            
            return cardPane;
        } catch (IOException e) {
            e.printStackTrace();
            return new StackPane();
        }
    }

    private StackPane createClosedCard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/Card.fxml"));
            StackPane cardPane = loader.load();
            CardController cardController = loader.getController();
            cardController.setCardBack();
            return cardPane;
        } catch (IOException e) {
            e.printStackTrace();
            return new StackPane();
        }
    }

    private CardData drawValidOpenCard(List<CardData> deck) {
        for (int i = 0; i < deck.size(); i++) {
            CardData card = deck.get(i);
            if (!(card.value.equals("Wild Draw Four"))) {
                deck.remove(i);
                return card;
            }
        }
        return new CardData("Wild", "black");
    }

    private List<CardData> createDeck() {
        List<CardData> deck = new ArrayList<>();
        for (String color : COLORS) {
            for (String value : VALUES) {
                deck.add(new CardData(value, color));
                deck.add(new CardData(value, color));
            }
        }
        for (int i = 0; i < 8; i++) {
            deck.add(new CardData("Wild", "black"));
            deck.add(new CardData("Wild Draw Four", "black"));
        }
        return deck;
    }

    @FXML
    private void handleBackToMainMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/menu.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            Stage stage = (Stage) backToMainMenu.getScene().getWindow();
            
            // Store current stage properties
            boolean wasMaximized = stage.isMaximized();
            double width = stage.getWidth();
            double height = stage.getHeight();
            double x = stage.getX();
            double y = stage.getY();
            
            // Get screen dimensions as fallback
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            
            // Create and set scene
            Scene scene = new Scene(root, width, height);
            stage.setScene(scene);
            
            // Restore position and size
            stage.setX(x);
            stage.setY(y);
            stage.setWidth(width);
            stage.setHeight(height);
            
            if (wasMaximized) {
                stage.setMaximized(true);
            }
            
            // If the window is smaller than the screen, resize it
            if (width < screenBounds.getWidth() || height < screenBounds.getHeight()) {
                stage.setX(screenBounds.getMinX());
                stage.setY(screenBounds.getMinY());
                stage.setWidth(screenBounds.getWidth());
                stage.setHeight(screenBounds.getHeight());
                stage.setMaximized(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addCardToCenterPile(CardData card) {
        centerPileBox.getChildren().clear();
        
        StackPane cardPane = createCardPane(card, -1);
        centerPileBox.getChildren().add(cardPane);
    }

    @FXML
    private void handleSkipCheat() {
        if (!isPlayerTurn) return;
        String currentColor = gameState.getCurrentColor();
        CardData card = new CardData("Skip", currentColor);
        gameState.playCard(0, card, currentColor);
        addCardToCenterPile(card);
        updateCurrentColorLabel();
        updateDirectionIndicator();
        isPlayerTurn = false;
        forceUpdateArrows();
        
        // Only play AI turns in single player mode
        if (!isMultiplayerGame) {
            playAITurns();
        } else {
            System.out.println("Multiplayer mode: AI turns disabled, waiting for other players...");
        }
    }

    @FXML
    private void handleReverseCheat() {
        if (!isPlayerTurn) return;
        String currentColor = gameState.getCurrentColor();
        CardData card = new CardData("Reverse", currentColor);
        gameState.playCard(0, card, currentColor);
        addCardToCenterPile(card);
        updateCurrentColorLabel();
        updateDirectionIndicator();
        isPlayerTurn = false;
        forceUpdateArrows();
        
        // Only play AI turns in single player mode
        if (!isMultiplayerGame) {
            playAITurns();
        } else {
            System.out.println("Multiplayer mode: AI turns disabled, waiting for other players...");
        }
    }

    @FXML
    private void handleDrawTwoCheat() {
        if (!isPlayerTurn) return;
        String currentColor = gameState.getCurrentColor();
        CardData card = new CardData("Draw Two", currentColor);
        gameState.playCard(0, card, currentColor);
        addCardToCenterPile(card);
        updateCurrentColorLabel();
        updateDirectionIndicator();
        isPlayerTurn = false;
        forceUpdateArrows();
        
        // Only play AI turns in single player mode
        if (!isMultiplayerGame) {
            playAITurns();
        } else {
            System.out.println("Multiplayer mode: AI turns disabled, waiting for other players...");
        }
    }

    @FXML
    private void handleWildCheat() {
        if (!isPlayerTurn) return;
        pendingWildCard = new CardData("Wild", "black");
        colorSelectionBox.setVisible(true);
    }

    @FXML
    private void handleWildDrawFourCheat() {
        if (!isPlayerTurn) return;
        pendingWildCard = new CardData("Wild Draw Four", "black");
        colorSelectionBox.setVisible(true);
    }

    @FXML
    private void handleColorSelection(MouseEvent event) {
        if (pendingWildCard == null) return;
        
        StackPane colorButton = (StackPane) event.getSource();
        String selectedColor = (String) colorButton.getUserData();
        
        if (isMultiplayerGame) {
            // In multiplayer mode, send wild card with chosen color to backend
            sendWildCardToServer(pendingWildCard, selectedColor);
            
            pendingWildCard = null;
            colorSelectionBox.setVisible(false);
            return;
        }
        
        // Single player mode logic
        Object pendingCardIndexObj = drawPileBox.getProperties().get("pendingCardIndex");
        if (pendingCardIndexObj != null) {
            // Regular card play from hand
            int cardIndex = (Integer) pendingCardIndexObj;
            List<CardData> playerHand = gameState.getPlayerHand(0);
            if (cardIndex >= 0 && cardIndex < playerHand.size()) {
                playerHand.remove(cardIndex);
                refreshPlayerHand();
            }
            gameState.playCard(0, pendingWildCard, selectedColor);
            
          
            if (gameState.getPlayerHand(0).isEmpty()) {
                playerWonGame = true;
                showGameOver("Congratulations! You Won!");
                addCardToCenterPile(pendingWildCard);
                updateCurrentColorLabel();
                updateDirectionIndicator();
                pendingWildCard = null;
                colorSelectionBox.setVisible(false);
                drawPileBox.getProperties().remove("pendingCardIndex");
                return;
            }
        } else {
            
            if (pendingWildCard.value.equals("Wild Draw Four")) {
                gameState.setCurrentColor(selectedColor);
                gameState.setDrawStack(4);
                gameState.moveToNextPlayer();
            } else {
                gameState.playCard(0, pendingWildCard, selectedColor);
            }
        }
        
        addCardToCenterPile(pendingWildCard);
        updateCurrentColorLabel();
        updateDirectionIndicator();
        
        pendingWildCard = null;
        colorSelectionBox.setVisible(false);
        drawPileBox.getProperties().remove("pendingCardIndex");
        isPlayerTurn = false;
        forceUpdateArrows();
        
        // Only play AI turns in single player mode
        if (!isMultiplayerGame) {
            playAITurns();
        } else {
            System.out.println("Multiplayer mode: AI turns disabled, waiting for other players...");
        }
    }
    
    private void sendWildCardToServer(CardData wildCard, String chosenColor) {
        try {
            WebSocketService webSocketService = context.getBean(WebSocketService.class);
            
            Map<String, String> cardMessage = new HashMap<>();
            cardMessage.put("gameId", gameRoomId);
            cardMessage.put("player", playerName);
            cardMessage.put("card", wildCard.color + "_" + wildCard.value);
            cardMessage.put("color", wildCard.color);
            cardMessage.put("chosenColor", chosenColor); // Send the chosen color for wild cards
            
            webSocketService.sendMessage("/app/playCard", cardMessage);
            System.out.println("Wild card sent to server: " + wildCard.color + "_" + wildCard.value + " with chosen color: " + chosenColor);
            
        } catch (Exception e) {
            System.err.println("Failed to send wild card to server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void playCard(CardData card, String color, int cardIndex) {
        System.out.println("Playing card at index: " + cardIndex);
        
        if (cardIndex >= 0) {
            // Regular card play from hand
            List<CardData> playerHand = gameState.getPlayerHand(0);
            System.out.println("Current hand size before playing: " + playerHand.size());
            
            if (cardIndex < playerHand.size()) {
                CardData clickedCard = playerHand.get(cardIndex);
                if (clickedCard.value.equals(card.value) && clickedCard.color.equals(card.color)) {
                    CardData cardToPlay = new CardData(clickedCard.value, clickedCard.color);
                    playerHand.remove(cardIndex);
                    gameState.playCard(0, cardToPlay, color);
                    refreshPlayerHand();
                    addCardToCenterPile(cardToPlay);
                    updateCurrentColorLabel();
                    updateDirectionIndicator();
                    
                    if (gameState.getPlayerHand(0).isEmpty()) {
                        playerWonGame = true;
                        showGameOver("Congratulations! You Won!");
                        return;
                    }
                } else {
                    System.out.println("Card at index " + cardIndex + " doesn't match the card being played!");
                    return;
                }
            } else {
                System.out.println("Invalid card index: " + cardIndex);
                return;
            }
        }
        
        isPlayerTurn = false;
        forceUpdateArrows();
        
        // Only play AI turns in single player mode
        if (!isMultiplayerGame) {
            playAITurns();
        } else {
            System.out.println("Multiplayer mode: AI turns disabled, waiting for other players...");
        }
    }

    @FXML
    private void handleUno() {
        if (!isPlayerTurn) return;
        
        if (gameState.getPlayerHand(0).size() == 1) {
            unoButton.setVisible(false);
        } else {
            showGameOver("You called UNO incorrectly! You must draw 2 cards.");
            gameState.drawCards(0);
            gameState.drawCards(0);
            
            playerHandBoxBottom.getChildren().clear();
            addCardsToHand(gameState.getPlayerHand(0), playerHandBoxBottom);
        }
    }

    @FXML
    public void setPlayerName(String name) {
        playerName = name;
        playerNameLabel.setText(playerName);
    }

    private void updateDirectionIndicator() {
        System.out.println("updateDirectionIndicator called, clockwise = " + gameState.isClockwise());
        if (directionIndicator != null && clockwiseImage != null && counterClockwiseImage != null) {
            boolean isClockwise = gameState.isClockwise();
            directionIndicator.setImage(isClockwise ? clockwiseImage : counterClockwiseImage);
            System.out.println("Direction indicator updated to: " + (isClockwise ? "clockwise" : "counter-clockwise"));
        } else {
            System.err.println("ERROR: directionIndicator or direction images are NULL");
        }
    }

  
    private void forceUpdateArrows() {
        Platform.runLater(() -> {
            try {
                hideAllArrows();
                
                int currentPlayerIndex = gameState.getCurrentPlayerIndex();
                
                System.out.println("player " + currentPlayerIndex + ", isPlayerTurn: " + isPlayerTurn);
                
                if (currentPlayerIndex == 0 && !isPlayerTurn) {
                    isPlayerTurn = true;
                } 
                else if (currentPlayerIndex != 0 && isPlayerTurn) {
                    isPlayerTurn = false;
                }
                
                switch (currentPlayerIndex) {
                    case 0:
                        if (bottomPlayerArrow != null) {
                            bottomPlayerArrow.setVisible(true);
                            bottomPlayerArrow.setOpacity(1.0);
                        }
                        break;
                    case 1: 
                        if (leftPlayerArrow != null) {
                            leftPlayerArrow.setVisible(true);
                            leftPlayerArrow.setOpacity(1.0);
                        }
                        break;
                    case 2: 
                        if (topPlayerArrow != null) {
                            topPlayerArrow.setVisible(true);
                            topPlayerArrow.setOpacity(1.0);
                        }
                        break;
                    case 3: 
                        if (rightPlayerArrow != null) {
                            rightPlayerArrow.setVisible(true);
                            rightPlayerArrow.setOpacity(1.0);
                        }
                        break;
                }
            } catch (Exception e) {
                System.err.println("arrow update error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        updateCheatButtonsVisibility();
    }
    
    private void hideAllArrows() {
        if (topPlayerArrow != null) {
            topPlayerArrow.setVisible(false);
        }
        if (bottomPlayerArrow != null) {
            bottomPlayerArrow.setVisible(false);
        }
        if (leftPlayerArrow != null) {
            leftPlayerArrow.setVisible(false);
        }
        if (rightPlayerArrow != null) {
            rightPlayerArrow.setVisible(false);
        }
    }
    
    private void loadArrowImages() {
        try {
            if (topPlayerArrow != null) {
                topPlayerArrow.setPreserveRatio(true);
                topPlayerArrow.getStyleClass().add("turn-arrow");
            }
            
            if (bottomPlayerArrow != null) {
                bottomPlayerArrow.setPreserveRatio(true);
                bottomPlayerArrow.getStyleClass().add("turn-arrow");
            }
            
            if (leftPlayerArrow != null) {
                leftPlayerArrow.setPreserveRatio(true);
                leftPlayerArrow.getStyleClass().add("turn-arrow");
            }
            
            if (rightPlayerArrow != null) {
                rightPlayerArrow.setPreserveRatio(true);
                rightPlayerArrow.getStyleClass().add("turn-arrow");
            }
            
            hideAllArrows();
        } catch (Exception e) {
            System.err.println("Error setting up arrows: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handlePlayAgain() {
        gameOverOverlay.setVisible(false);
        initializeSinglePlayer();
        setPlayerName(com.example.unofrontend.session.SessionManager.getUsername());
        
        Stage stage = (Stage) backToMainMenu.getScene().getWindow();
        
        // Get screen dimensions
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        

        stage.setX(screenBounds.getMinX());
        stage.setY(screenBounds.getMinY());
        stage.setWidth(screenBounds.getWidth());
        stage.setHeight(screenBounds.getHeight());
        

        stage.setMaximized(true);
    }

    private void updateUnoIndicator(int playerIndex, int cardCount) {
        Label indicator = switch (playerIndex) {
            case 0 -> playerUnoIndicator;
            case 1 -> player2UnoIndicator;
            case 2 -> player3UnoIndicator;
            case 3 -> player4UnoIndicator;
            default -> null;
        };
        if (indicator != null) {
            indicator.setVisible(cardCount > 0);
            if (cardCount == 1) {
                indicator.getStyleClass().add("uno-warning");
            } else {
                indicator.getStyleClass().remove("uno-warning");
            }
            System.out.println("UNO indicator for player " + playerIndex + " set visible=" + indicator.isVisible() + ", cardCount=" + cardCount);
        }
    }

    private void updateCheatButtonsVisibility() {
        // Hide cheat buttons in multiplayer mode
        boolean isVisible = isPlayerTurn && !isMultiplayerGame;
        cheatButtonsBox.setVisible(isVisible);
        skipCheatButton.setVisible(isVisible);
        reverseCheatButton.setVisible(isVisible);
        drawTwoCheatButton.setVisible(isVisible);
        wildCheatButton.setVisible(isVisible);
        wildDrawFourCheatButton.setVisible(isVisible);
    }


    public void connectToWebSocket() {
        try {
            System.out.println("WebSocket connetion is starting...");
            updateWebSocketStatus("WebSocket: Attempting to connect...");
            
            WebSocketService webSocketService = context.getBean(WebSocketService.class);
            String serverUrl = apiService.getBaseUrl() + "/uno-websocket";
            
            webSocketService.connect(serverUrl, (status) -> {
                Platform.runLater(() -> {
                    updateWebSocketStatus("WebSocket: " + status);
                    if (status.contains("Connected")) {
                        isWebSocketConnected = true;
                        
                        // Use existing room ID if available, otherwise create new one
                        if (gameRoomId == null || gameRoomId.trim().isEmpty()) {
                            gameRoomId = "game-" + System.currentTimeMillis();
                            System.out.println("Created new game room ID: " + gameRoomId);
                        } else {
                            System.out.println("Using existing room ID: " + gameRoomId);
                        }
                        
                        String playerName = SessionManager.getUsername() != null ? 
                                          SessionManager.getUsername() : "Player1";
                        
                        // Subscribe to individual game state (with playerIndex)
                        webSocketService.subscribeToGameState((message) -> {
                            System.out.println("Received WebSocket message: " + message.get("type"));
                            Platform.runLater(() -> handleWebSocketMessage(message));
                        });
                        
                        // Join the game room
                        webSocketService.joinGame(gameRoomId, playerName);
                        
                        // Send start game request after WebSocket connection is established
                        // Any player can send this request, backend will validate if game can start
                        if (isMultiplayerGame) {
                            try {
                                Thread.sleep(1000); // Wait a moment for subscription to complete
                                sendStartGameRequest();
                                System.out.println("Start game request sent after WebSocket connection");
                            } catch (InterruptedException e) {
                                System.err.println("Sleep interrupted: " + e.getMessage());
                            }
                        }
                        
                        System.out.println("WebSocket is connected. Game ID: " + gameRoomId);
                    } else {
                        isWebSocketConnected = false;
                        updateWebSocketStatus("WebSocket: Connection failed");
                    }
                });
            });
            
        } catch (Exception e) {
            System.out.println("WebSocket connection error: " + e.getMessage());
            e.printStackTrace();
            updateWebSocketStatus("WebSocket: Connection failed");
            isWebSocketConnected = false;
        }
    }


    public void connectToWebSocketWithRoom(String roomId) {
        try {
            System.out.println("WebSocket connection is starting... Room: " + roomId);
            updateWebSocketStatus("WebSocket: Attempting to connect...");
            // Set room ID BEFORE connecting
            this.gameRoomId = roomId;
            WebSocketService webSocketService = context.getBean(WebSocketService.class);
            // Check if already connected (from MenuController)
            if (webSocketService.isConnected()) {
                System.out.println("WebSocket already connected, reusing existing connection");
                updateWebSocketStatus("WebSocket: Connected (reused)");
                isWebSocketConnected = true;
                // Set up game subscription for this specific room
                setupGameStateSubscription(webSocketService);
                // Only the creator should send the start game request
                if (isRoomCreator) {
                    Timeline delayedStartRequest = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                        sendStartGameRequest();
                        System.out.println("Start game request sent after reusing WebSocket connection for room: " + roomId);
                    }));
                    delayedStartRequest.play();
                }
                return;
            }
            // If not connected, create new connection
            String serverUrl = apiService.getBaseUrl() + "/uno-websocket";
            webSocketService.connect(serverUrl, (status) -> {
                Platform.runLater(() -> {
                    updateWebSocketStatus("WebSocket: " + status);
                    if (status.contains("Connected")) {
                        isWebSocketConnected = true;
                        System.out.println("Using backend room ID: " + roomId);
                        String playerName = SessionManager.getUsername() != null ? 
                                          SessionManager.getUsername() : "Player1";
                        // Set up game subscription for this specific room
                        setupGameStateSubscription(webSocketService);
                        // Join the game room
                        webSocketService.joinGame(roomId, playerName);
                        // Only the creator should send the start game request
                        if (isRoomCreator) {
                            Timeline delayedStartRequest = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                                sendStartGameRequest();
                                System.out.println("Start game request sent after WebSocket connection for room: " + roomId);
                            }));
                            delayedStartRequest.play();
                        }
                    } else {
                        isWebSocketConnected = false;
                        updateWebSocketStatus("WebSocket: Connection failed");
                    }
                });
            });
        } catch (Exception e) {
            System.out.println("WebSocket connection error: " + e.getMessage());
            e.printStackTrace();
            updateWebSocketStatus("WebSocket: Connection failed");
            isWebSocketConnected = false;
        }
    }
    

    private void handleWebSocketMessage(Map<String, Object> message) {
        Platform.runLater(() -> {
            String type = (String) message.get("type");
            System.out.println("[WebSocket] Received message type: " + type);
            
            switch (type) {
                case "GAME_STARTED":
                case "GAME_STATE":
                    System.out.println("[WebSocket] " + type + " event received, calling handleMultiplayerGameStarted");
                    handleMultiplayerGameStarted(message);
                    break;
                case "CARD_PLAYED":
                    handleMultiplayerCardPlayed(message);
                    break;
                case "CARDS_DRAWN":
                    handleMultiplayerCardsDrawn(message);
                    break;
                case "GAME_OVER":
                    handleMultiplayerGameOver(message);
                    break;
                case "ERROR":
                case "INVALID_MOVE":
                    String errorMessage = (String) message.get("message");
                    System.out.println("Game error: " + errorMessage);
                    break;
                default:
                    System.out.println("Unknown message type: " + type);
            }
        });
    }
    
    @SuppressWarnings("unchecked")
    private void handleMultiplayerGameStarted(Map<String, Object> message) {
        try {
            System.out.println("[handleMultiplayerGameStarted] Called with message: " + message);
            // Get player data
            List<String> players = (List<String>) message.get("players");
            List<Map<String, Object>> playerHand = (List<Map<String, Object>>) message.get("playerHand");
            Map<String, Integer> handSizes = (Map<String, Integer>) message.get("handSizes");
            String currentPlayer = (String) message.get("currentPlayer");
            String topCard = (String) message.get("topCard");
            String currentColor = (String) message.get("currentColor");
            Integer direction = (Integer) message.get("direction");
            Integer playerIndex = (Integer) message.get("playerIndex");

            System.out.println("players: " + players + ", playerIndex: " + playerIndex + ", currentPlayer: " + currentPlayer + ", playerName: " + playerName);

            if (players == null || playerIndex == null) {
                System.err.println("ERROR: players or playerIndex is null, skipping rendering!");
                return;
            }

            // Set turn status
            isPlayerTurn = currentPlayer != null && currentPlayer.equals(playerName);
            System.out.println("isPlayerTurn: " + isPlayerTurn);
            
            // Clear all hands first
            playerHandBoxBottom.getChildren().clear();
            opponentHandBoxLeft.getChildren().clear();
            playerHandBoxTop.getChildren().clear(); 
            opponentHandBoxRight.getChildren().clear();
            
            // Add my cards to bottom hand
            if (playerHand != null && !playerHand.isEmpty()) {
                List<CardData> myCards = new ArrayList<>();
                for (Map<String, Object> cardMap : playerHand) {
                    String value = (String) cardMap.get("value");
                    String color = (String) cardMap.get("color");
                    myCards.add(new CardData(value, color));
                }
                addCardsToHand(myCards, playerHandBoxBottom);
                System.out.println("Added " + myCards.size() + " cards to my hand");
            } else {
                System.out.println("Warning: No cards received for my hand");
            }
            

            if (handSizes != null && players != null && playerIndex != null) {
                for (int i = 0; i < players.size(); i++) {
                    if (i != playerIndex.intValue()) { // Skip my own index
                        String player = players.get(i);
                        int handSize = handSizes.getOrDefault(player, 7);
                        Pane targetHandBox = getHandBoxForPlayerIndex(i, playerIndex, players.size());
                        System.out.println("Mapping: player='" + player + "' index=" + i + " handSize=" + handSize + " -> handBox=" + (targetHandBox != null ? targetHandBox.getId() : "null"));
                        if (targetHandBox != null) {
                            addPlaceholderCards(targetHandBox, handSize);
                            // Update UNO indicator using the correct mapping
                            Label unoIndicator = getUnoIndicatorForPlayerIndex(i, playerIndex, players.size());
                            if (unoIndicator != null) {
                                unoIndicator.setVisible(handSize > 0);
                                if (handSize == 1) {
                                    unoIndicator.getStyleClass().add("uno-warning");
                                    unoIndicator.setText("UNO!");
                                } else {
                                    unoIndicator.getStyleClass().remove("uno-warning");
                                    unoIndicator.setText("UNO");
                                }
                                unoIndicator.toFront();
                                System.out.println("[handleMultiplayerGameStarted] Updated UNO indicator for player " + i + " with handSize=" + handSize);
                            }
                        }
                    }
                }
                // Always update your own UNO indicator
                int myHandSize = handSizes.getOrDefault(players.get(playerIndex), 0);
                playerUnoIndicator.setVisible(myHandSize > 0);
                if (myHandSize == 1) {
                    playerUnoIndicator.getStyleClass().add("uno-warning");
                    playerUnoIndicator.setText("UNO!");
                } else {
                    playerUnoIndicator.getStyleClass().remove("uno-warning");
                    playerUnoIndicator.setText("UNO");
                }
                playerUnoIndicator.toFront();
            }
            
            // Set top card
            if (topCard != null && !topCard.isEmpty()) {
                String[] parts = topCard.split("_");
                if (parts.length >= 2) {
                    CardData topCardData = new CardData(parts[1], parts[0]);
                    centerPileBox.getChildren().clear();
                    addCardToCenterPile(topCardData);
                    System.out.println("Set top card: " + topCardData.value + " of " + topCardData.color);
                }
            }
            
            // Set current color
            if (currentColor != null) {
                updateCurrentColorDisplay(currentColor);
                // Also update the GameState
                if (gameState != null) {
                    gameState.setCurrentColor(currentColor);
                }
                System.out.println("Set current color: " + currentColor);
            }
            
            // Update direction indicator
            if (direction != null) {
                boolean clockwise = direction == 1;
                updateDirectionDisplay(clockwise);
                // Also update the GameState
                if (gameState != null) {
                    // Use reverseDirection if needed to set direction
                    if (clockwise != gameState.isClockwise()) {
                        gameState.reverseDirection();
                    }
                }
                System.out.println("Set direction: " + (clockwise ? "clockwise" : "counter-clockwise"));
            }
            
            // Update turn arrows
            if (playerIndex != null && players != null) {
                updateTurnArrows(currentPlayer, players, playerIndex);
                System.out.println("Updated turn arrows - current player: " + currentPlayer + ", my turn: " + isPlayerTurn);
            }
            
            // Update player names in multiplayer mode
            if (players != null && playerIndex != null) {
                updateMultiplayerPlayerNames(players, playerIndex);
            }
            
            // Update cheat button visibility for multiplayer
            updateCheatButtonsVisibility();
            
            System.out.println("Multiplayer game UI updated successfully");
            
        } catch (Exception e) {
            System.err.println("Error handling game started: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private Pane getHandBoxForPlayerIndex(int otherPlayerIndex, int myIndex, int totalPlayers) {
        if (totalPlayers == 2) {
            // In 2-player game, opponent is always at the top
            return otherPlayerIndex == myIndex ? playerHandBoxBottom : playerHandBoxTop;
        } else if (totalPlayers == 3) {
            int relativePos = (otherPlayerIndex - myIndex + totalPlayers) % totalPlayers;
            switch (relativePos) {
                case 1: return opponentHandBoxLeft; // Next player
                case 2: return playerHandBoxTop;    // Opposite player
                default: return opponentHandBoxRight;
            }
        } else { // 4 players
            int relativePos = (otherPlayerIndex - myIndex + totalPlayers) % totalPlayers;
            switch (relativePos) {
                case 1: return opponentHandBoxLeft;
                case 2: return playerHandBoxTop;
                case 3: return opponentHandBoxRight;
                default: return null;
            }
        }
    }
    
    private void updateCurrentColorDisplay(String color) {
        if (currentColorRect != null) {
            currentColorRect.getStyleClass().removeAll("red-square", "blue-square", "green-square", "yellow-square");
            currentColorRect.getStyleClass().add(color.toLowerCase() + "-square");
        }
    }
    
    private void updateDirectionDisplay(boolean clockwise) {
        // Update direction indicator if you have one
        if (directionIndicator != null) {
            if (clockwise && clockwiseImage != null) {
                directionIndicator.setImage(clockwiseImage);
            } else if (!clockwise && counterClockwiseImage != null) {
                directionIndicator.setImage(counterClockwiseImage);
            }
        }
    }
    
    private void updateTurnArrows(String currentPlayer, List<String> players, int myIndex) {
        hideAllArrows();
        
        if (currentPlayer.equals(playerName)) {
            // My turn
            bottomPlayerArrow.setVisible(true);
            isPlayerTurn = true;
        } else {
            // Find which opponent has the turn
            int currentPlayerIndex = players.indexOf(currentPlayer);
            if (currentPlayerIndex != -1) {
                Pane handBox = getHandBoxForPlayerIndex(currentPlayerIndex, myIndex, players.size());
                if (handBox == playerHandBoxTop) {
                    topPlayerArrow.setVisible(true);
                } else if (handBox == opponentHandBoxLeft) {
                    leftPlayerArrow.setVisible(true);
                } else if (handBox == opponentHandBoxRight) {
                    rightPlayerArrow.setVisible(true);
                }
            }
            isPlayerTurn = false;
        }
    }
    
    @SuppressWarnings("unchecked")
    private void handleMultiplayerCardPlayed(Map<String, Object> message) {
        // Similar to handleMultiplayerGameStarted but for card updates
        System.out.println("Card played in multiplayer game");
        handleMultiplayerGameStarted(message);
    }
    
    @SuppressWarnings("unchecked")
    private void handleMultiplayerCardsDrawn(Map<String, Object> message) {
        // Similar to handleMultiplayerGameStarted but for draw updates
        System.out.println("Cards drawn in multiplayer game");
        handleMultiplayerGameStarted(message);
    }
    
    private void handleMultiplayerGameOver(Map<String, Object> message) {
        String winner = (String) message.get("winner");
        if (winner != null) {
            String gameOverMessage = winner.equals(playerName) ? "You Won!" : winner + " Won!";
            showGameOver(gameOverMessage);
        }
    }


    private void updateWebSocketStatus(String status) {
        if (webSocketStatusLabel != null) {
            webSocketStatusLabel.setText(status);
            if (status.contains("Connected") || status.contains("Message received")) {
                webSocketStatusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            } else if (status.contains("Connecting")) {
                webSocketStatusLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
            } else {
                webSocketStatusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            }
        }
    }

    private void updateMultiplayerPlayerNames(List<String> players, int myIndex) {
        // Hide all player labels initially
        if (player2Label != null) player2Label.setVisible(false);
        if (player3Label != null) player3Label.setVisible(false);
        if (player4Label != null) player4Label.setVisible(false);
        
        if (players == null || players.size() <= 1) {
            return; // No other players to show
        }
        
        // Update player names based on their relative positions
        for (int i = 0; i < players.size(); i++) {
            if (i != myIndex) { // Skip my own index
                String playerName = players.get(i);
                Pane handBox = getHandBoxForPlayerIndex(i, myIndex, players.size());
                
                // Determine which label to update based on hand box position
                if (handBox == playerHandBoxTop && player3Label != null) {
                    player3Label.setText(playerName.toUpperCase());
                    player3Label.setVisible(true);
                    System.out.println("Set top player name: " + playerName);
                } else if (handBox == opponentHandBoxLeft && player2Label != null) {
                    player2Label.setText(playerName.toUpperCase());
                    player2Label.setVisible(true);
                    System.out.println("Set left player name: " + playerName);
                } else if (handBox == opponentHandBoxRight && player4Label != null) {
                    player4Label.setText(playerName.toUpperCase());
                    player4Label.setVisible(true);
                    System.out.println("Set right player name: " + playerName);
                }
            }
        }
    }

    private Label getUnoIndicatorForPlayerIndex(int otherPlayerIndex, int myIndex, int totalPlayers) {
        if (totalPlayers == 2) {
            // You: always index 0 (bottom), Opponent: always index 1 (top)
            return otherPlayerIndex == myIndex ? playerUnoIndicator : player3UnoIndicator;
        } else if (totalPlayers == 3) {
            int rel = (otherPlayerIndex - myIndex + totalPlayers) % totalPlayers;
            return switch (rel) {
                case 1 -> player2UnoIndicator;
                case 2 -> player3UnoIndicator;
                default -> player4UnoIndicator;
            };
        } else { // 4 players
            int rel = (otherPlayerIndex - myIndex + totalPlayers) % totalPlayers;
            return switch (rel) {
                case 1 -> player2UnoIndicator;
                case 2 -> player3UnoIndicator;
                case 3 -> player4UnoIndicator;
                default -> playerUnoIndicator;
            };
        }
    }
} 