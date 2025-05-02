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

    @FXML
    private ImageView drawPileLogo;

    private CardData pendingWildCard = null;

    private final ApplicationContext context;

    private Image clockwiseImage;
    private Image counterClockwiseImage;

    @Autowired
    private ApiService apiService;

    public GameBoardController(ApplicationContext context) {
        this.context = context;
    }

    @FXML
    public void initialize() {
        System.out.println("Game board initialized");
        try {
            clockwiseImage = new Image(getClass().getResourceAsStream("/images/rotate-right.png"));
            counterClockwiseImage = new Image(getClass().getResourceAsStream("/images/rotate-left.png"));
            
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

    private void updateCurrentColorLabel() {
        String currentColor = gameState.getCurrentColor();
        currentColorRect.getStyleClass().removeAll("red-square", "blue-square", "green-square", "yellow-square");
        currentColorRect.getStyleClass().add(currentColor.toLowerCase() + "-square");
    }

    private void handleDrawPileClick() {
        if (!isPlayerTurn) return;

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

    private void refreshPlayerHand() {
        List<CardData> playerHand = gameState.getPlayerHand(0);
        playerHandBoxBottom.getChildren().clear();
        addCardsToHand(playerHand, playerHandBoxBottom);
    }

    private void handleCardClick(CardData card, int cardIndex) {
        if (!isPlayerTurn) return;
        
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
        gameOverMessage.setText(message);
        gameOverOverlay.setVisible(true);
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
            updateUnoIndicator(playerIndex, cards.size());
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
            Scene scene = new Scene(root);
            
           
            stage.setScene(scene);
            
         
            stage.sizeToScene();  
            
            Screen screen = Screen.getPrimary();
            Rectangle2D bounds = screen.getVisualBounds();
            
            double centerX = bounds.getMinX() + (bounds.getWidth() - stage.getWidth()) / 2;
            double centerY = bounds.getMinY() + (bounds.getHeight() - stage.getHeight()) / 2;
            
           
            stage.setX(centerX);
            stage.setY(centerY);
            
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
        playAITurns();
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
        playAITurns();
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
        playAITurns();
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
                String username = SessionManager.getUsername();
                if (username != null && !username.isEmpty()) {
                    String result = apiService.updateDailyScore(1);
                    if (!"success".equals(result)) {
                        System.err.println("Failed to update daily score: " + result);
                    }
                }
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
        playAITurns();
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
                        
                        String username = SessionManager.getUsername();
                        if (username != null && !username.isEmpty()) {
                            String result = apiService.updateDailyScore(1);
                            if (!"success".equals(result)) {
                                System.err.println("Failed to update daily score: " + result);
                            }
                        }
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
        playAITurns();
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
        }
    }

    private void updateCheatButtonsVisibility() {
        boolean isVisible = isPlayerTurn;
        cheatButtonsBox.setVisible(isVisible);
        skipCheatButton.setVisible(isVisible);
        reverseCheatButton.setVisible(isVisible);
        drawTwoCheatButton.setVisible(isVisible);
        wildCheatButton.setVisible(isVisible);
        wildDrawFourCheatButton.setVisible(isVisible);
    }
} 