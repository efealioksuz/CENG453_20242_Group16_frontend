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

@Component
public class GameBoardController {
    @FXML
    private Button backToLogin;
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
    private TurnIndicatorController turnIndicatorController;
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

    @FXML
    private ImageView drawPileLogo;

    private CardData pendingWildCard = null;

    private final ApplicationContext context;

    public GameBoardController(ApplicationContext context) {
        this.context = context;
    }

    @FXML
    public void initialize() {
        System.out.println("Game board initialized");
        try {
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
            
        } catch (Exception e) {
            System.err.println("Error setting UNO logo: " + e.getMessage());
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
                
                if (!gameState.canPlayCard(drawnCard)) {
                    gameState.moveToNextPlayer();
                    isPlayerTurn = false;
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
        System.out.println("Starting AI turns");
        int nextPlayerIndex = gameState.getCurrentPlayerIndex();
        playNextAITurn(nextPlayerIndex);
    }

    private void playNextAITurn(int playerIndex) {
        if (playerIndex >= 4 || playerIndex == 0) {
            isPlayerTurn = true;
            if (turnIndicatorController != null) {
                turnIndicatorController.setCurrentPlayer("Your Turn");
            }
            System.out.println("AI turns completed, returning control to player");
            
            if (gameState.getDrawStack() > 0) {
                checkAndHandleForcedDraws();
            }
            return;
        }

        String playerName;
        switch (playerIndex) {
            case 1: playerName = "Player 2"; break;
            case 2: playerName = "Player 3"; break;
            case 3: playerName = "Player 4"; break;
            default: playerName = "Unknown Player";
        }
        if (turnIndicatorController != null) {
            turnIndicatorController.setCurrentPlayer(playerName + "'s Turn");
        }

        Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1.0), e -> {
            List<CardData> aiHand = gameState.getPlayerHand(playerIndex);
            
            if (gameState.getDrawStack() > 0) {
                System.out.println(playerName + " needs to draw " + gameState.getDrawStack() + " cards");

                CardData drawCard = aiPlayer.chooseDrawCardToPlay(aiHand, gameState);
                if (drawCard != null) {
                    System.out.println(playerName + " is playing: " + drawCard.value + " of " + drawCard.color);
                    
                    String chosenColor = null;
                    if (drawCard.value.equals("Wild") || drawCard.value.equals("Wild Draw Four")) {
                        chosenColor = aiPlayer.chooseColor(aiHand);
                        System.out.println(playerName + " chose color: " + chosenColor);
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
                        aiHand.remove(cardIndex);
                        CardData cardToPlay = new CardData(drawCard.value, drawCard.color);
                        gameState.playCard(playerIndex, cardToPlay, chosenColor);
                        
                        Pane handBox = (playerIndex == 1) ? opponentHandBoxLeft : 
                                     (playerIndex == 2) ? playerHandBoxTop : opponentHandBoxRight;
                        handBox.getChildren().clear();
                        addCardsToHand(aiHand, handBox);
                        
                        addCardToCenterPile(cardToPlay);
                        updateCurrentColorLabel();

                        if (aiHand.isEmpty()) {
                            showGameOver("Game Over! " + playerName + " Won!");
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
                System.out.println(playerName + " is playing: " + chosenCard.value + " of " + chosenCard.color);
                
                String chosenColor = null;
                if (chosenCard.value.equals("Wild") || chosenCard.value.equals("Wild Draw Four")) {
                    chosenColor = aiPlayer.chooseColor(aiHand);
                    System.out.println(playerName + " chose color: " + chosenColor);
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
                    aiHand.remove(cardIndex);
                    CardData cardToPlay = new CardData(chosenCard.value, chosenCard.color);
                    gameState.playCard(playerIndex, cardToPlay, chosenColor);

                    Pane handBox = (playerIndex == 1) ? opponentHandBoxLeft : 
                                 (playerIndex == 2) ? playerHandBoxTop : opponentHandBoxRight;
                    handBox.getChildren().clear();
                    addCardsToHand(aiHand, handBox);
                
                    addCardToCenterPile(cardToPlay);
                    updateCurrentColorLabel();

                    if (aiHand.isEmpty()) {
                        showGameOver("Game Over! " + playerName + " Won!");
                        return;
                    }
                }

                playNextAITurn(gameState.getCurrentPlayerIndex());
            } else {
                System.out.println(playerName + " has no playable cards, drawing one card");
                CardData drawnCard = gameState.drawCard();
                if (drawnCard != null) {
                    System.out.println(playerName + " drew: " + drawnCard.value + " of " + drawnCard.color);
                    aiHand.add(drawnCard);
                    
                    Pane handBox = (playerIndex == 1) ? opponentHandBoxLeft : 
                                 (playerIndex == 2) ? playerHandBoxTop : opponentHandBoxRight;
                    handBox.getChildren().clear();
                    addCardsToHand(aiHand, handBox);
                    
                    if (gameState.canPlayCard(drawnCard)) {
                        Timeline showDrawnCardTimeline = new Timeline();
                        showDrawnCardTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1.5), showEvent -> {
                            String chosenColor = null;
                            if (drawnCard.value.equals("Wild") || drawnCard.value.equals("Wild Draw Four")) {
                                chosenColor = aiPlayer.chooseColor(aiHand);
                                System.out.println(playerName + " chose color: " + chosenColor);
                            } else {
                                chosenColor = drawnCard.color;
                            }
                            
                            int cardIndex = -1;
                            for (int i = 0; i < aiHand.size(); i++) {
                                CardData card = aiHand.get(i);
                                if (card.value.equals(drawnCard.value) && card.color.equals(drawnCard.color)) {
                                    cardIndex = i;
                                    break;
                                }
                            }
                            
                            if (cardIndex != -1) {
                                aiHand.remove(cardIndex);
                                CardData cardToPlay = new CardData(drawnCard.value, drawnCard.color);
                                gameState.playCard(playerIndex, cardToPlay, chosenColor);
                                
                                handBox.getChildren().clear();
                                addCardsToHand(aiHand, handBox);
                                
                                addCardToCenterPile(cardToPlay);
                                updateCurrentColorLabel();

                                if (aiHand.isEmpty()) {
                                    showGameOver("Game Over! " + playerName + " Won!");
                                    return;
                                }
                            }

                            playNextAITurn(gameState.getCurrentPlayerIndex());
                        }));
                        showDrawnCardTimeline.play();
                    } else {
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
    private void backToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) backToLogin.getScene().getWindow();
            stage.setScene(new Scene(root));
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
    private void handleColorSelection(MouseEvent event) {
        if (pendingWildCard == null) return;
        
        StackPane colorButton = (StackPane) event.getSource();
        String selectedColor = (String) colorButton.getUserData();
        
        Integer cardIndex = (Integer) drawPileBox.getProperties().get("pendingCardIndex");
        playCard(pendingWildCard, selectedColor, cardIndex);
        pendingWildCard = null;
        colorSelectionBox.setVisible(false);
        drawPileBox.getProperties().remove("pendingCardIndex");
    }

    private void playCard(CardData card, String color, int cardIndex) {
        System.out.println("Playing card at index: " + cardIndex);
        System.out.println("Current hand size before playing: " + gameState.getPlayerHand(0).size());
        
        List<CardData> playerHand = gameState.getPlayerHand(0);
        
        if (cardIndex >= 0 && cardIndex < playerHand.size()) {
            CardData clickedCard = playerHand.get(cardIndex);
            if (clickedCard.value.equals(card.value) && clickedCard.color.equals(card.color)) {
                CardData cardToPlay = new CardData(clickedCard.value, clickedCard.color);
                playerHand.remove(cardIndex);
                gameState.playCard(0, cardToPlay, color);
                refreshPlayerHand();
                addCardToCenterPile(cardToPlay);
                updateCurrentColorLabel();
                
                if (gameState.getPlayerHand(0).isEmpty()) {
                    showGameOver("Congratulations! You Won! 🎉");
                    return;
                }
                
                isPlayerTurn = false;
                playAITurns();
            } else {
                System.out.println("Card at index " + cardIndex + " doesn't match the card being played!");
            }
        } else {
            System.out.println("Invalid card index: " + cardIndex);
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
        if (turnIndicatorController != null) {
            turnIndicatorController.setPlayerName(name);
        }
    }

    @FXML
    private void handlePlayAgain() {
        gameOverOverlay.setVisible(false);
        initializeSinglePlayer();
        setPlayerName(com.example.unofrontend.session.SessionManager.getUsername());
    }

    @FXML
    private void handleBackToGameMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/menu.fxml"));
            loader.setControllerFactory(context::getBean);  // Set Spring controller factory
            Parent root = loader.load();
            Stage stage = (Stage) gameOverOverlay.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
} 