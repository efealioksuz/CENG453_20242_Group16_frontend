package com.example.unofrontend.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private Button drawCardButton;
    @FXML
    private ImageView drawPileLogo;

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
    public void initialize() {
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
        } catch (Exception e) {
            System.err.println("Error setting UNO logo: " + e.getMessage());
        }
    }

    public void initializeSinglePlayer() {
        gameState = new GameState();
        aiPlayer = new AIPlayer();
        isPlayerTurn = true;
        
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
        if (currentColorLabel != null) {
            currentColorLabel.setText("Current Color: " + gameState.getCurrentColor());
        }
    }

    private void handleCardClick(CardData card) {
        if (!isPlayerTurn) return;
        
        if (gameState.getDrawStack() > 0) {
            CardData topCard = gameState.getTopCard();
            
            if (topCard.value.equals("Wild Draw Four")) {
                gameState.drawCards(0);

                playerHandBoxBottom.getChildren().clear();
                addCardsToHand(gameState.getPlayerHand(0), playerHandBoxBottom);
                
                isPlayerTurn = false;
                playAITurns();
                return;
            }
            
            if (topCard.value.equals("Draw Two")) {
                boolean hasDrawCard = false;
                for (CardData handCard : gameState.getPlayerHand(0)) {
                    if (handCard.value.equals("Draw Two")) {
                        hasDrawCard = true;
                        break;
                    }
                }
                
                if (!hasDrawCard) {
                    gameState.drawCards(0);
                    playerHandBoxBottom.getChildren().clear();
                    addCardsToHand(gameState.getPlayerHand(0), playerHandBoxBottom);
                    isPlayerTurn = false;
                    playAITurns();
                    return;
                }
            }
        }
        
        if (gameState.canPlayCard(card)) {
            System.out.println("Playing card: " + card.value + " of " + card.color);
            System.out.println("Current hand size before playing: " + gameState.getPlayerHand(0).size());
            
            gameState.playCard(0, card, card.color);
            
            List<CardData> playerHand = gameState.getPlayerHand(0);
            playerHand.removeIf(c -> c.value.equals(card.value) && c.color.equals(card.color));
            
            playerHandBoxBottom.getChildren().removeIf(node -> {
                if (node instanceof StackPane) {
                    CardController cardController = (CardController) node.getUserData();
                    if (cardController != null) {
                        CardData cardData = cardController.getCardData();
                        boolean matches = cardData.value.equals(card.value) && cardData.color.equals(card.color);
                        if (matches) {
                            System.out.println("Removing card from UI: " + cardData.value + " of " + cardData.color);
                        }
                        return matches;
                    }
                }
                return false;
            });
            
            System.out.println("Current hand size after playing: " + gameState.getPlayerHand(0).size());
            
            addCardToCenterPile(card);
            
            updateCurrentColorLabel();
            
            if (gameState.getPlayerHand(0).isEmpty()) {
                showGameOver("You won!");
                return;
            }
            
            isPlayerTurn = false;
            
            playAITurns();
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
            return;
        }

        System.out.println("AI Player " + playerIndex + "'s turn");
        if (gameState.getPlayerHand(playerIndex).isEmpty()) {
            showGameOver("Player " + playerIndex + " won!");
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
        
        timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1.5), e -> {
            if (gameState.getDrawStack() > 0) {
                System.out.println(playerName + " needs to draw " + gameState.getDrawStack() + " cards");

                CardData drawCard = aiPlayer.chooseDrawCardToPlay(gameState.getPlayerHand(playerIndex), gameState);
                if (drawCard != null) {
                    System.out.println(playerName + " is playing: " + drawCard.value + " of " + drawCard.color);
                    
                    String chosenColor = aiPlayer.chooseColor(gameState.getPlayerHand(playerIndex));
                    System.out.println(playerName + " chose color: " + chosenColor);
                    gameState.playCard(playerIndex, drawCard, chosenColor);
                    
                    Pane handBox = (playerIndex == 1) ? opponentHandBoxLeft : 
                                 (playerIndex == 2) ? playerHandBoxTop : opponentHandBoxRight;
                    handBox.getChildren().clear();
                    addCardsToHand(gameState.getPlayerHand(playerIndex), handBox);
                    
                    addCardToCenterPile(drawCard);
                    
                    updateCurrentColorLabel();
                    Timeline nextTurnTimeline = new Timeline();
                    nextTurnTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1.5), event -> {
                        int nextPlayerIndex = gameState.getCurrentPlayerIndex();
                        if (nextPlayerIndex == 0) {
                            isPlayerTurn = true;
                            if (turnIndicatorController != null) {
                                turnIndicatorController.setCurrentPlayer("Your Turn");
                            }
                            System.out.println("AI turns completed, returning control to player");
                        } else {
                            playNextAITurn(nextPlayerIndex);
                        }
                    }));
                    nextTurnTimeline.play();
                } else {
                    gameState.drawCards(playerIndex);
                    
                    Pane handBox = (playerIndex == 1) ? opponentHandBoxLeft : 
                                 (playerIndex == 2) ? playerHandBoxTop : opponentHandBoxRight;
                    handBox.getChildren().clear();
                    addCardsToHand(gameState.getPlayerHand(playerIndex), handBox);
                    
                    Timeline nextTurnTimeline = new Timeline();
                    nextTurnTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1.5), event -> {
                        int nextPlayerIndex = gameState.getCurrentPlayerIndex();
                        if (nextPlayerIndex == 0) {
                            isPlayerTurn = true;
                            if (turnIndicatorController != null) {
                                turnIndicatorController.setCurrentPlayer("Your Turn");
                            }
                            System.out.println("AI turns completed, returning control to player");
                        } else {
                            playNextAITurn(nextPlayerIndex);
                        }
                    }));
                    nextTurnTimeline.play();
                }
                return;
            }

            CardData chosenCard = aiPlayer.chooseCardToPlay(gameState.getPlayerHand(playerIndex), gameState);
            
            if (chosenCard != null) {
                System.out.println(playerName + " is playing: " + chosenCard.value + " of " + chosenCard.color);
                
                Timeline playCardTimeline = new Timeline();
                playCardTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1.0), ev -> {
                    String chosenColor = aiPlayer.chooseColor(gameState.getPlayerHand(playerIndex));
                    System.out.println(playerName + " chose color: " + chosenColor);
                    gameState.playCard(playerIndex, chosenCard, chosenColor);

                    if (chosenCard.value.equals("Wild") || chosenCard.value.equals("Wild Draw Four")) {
                        System.out.println(playerName + " chose " + chosenColor + " color");
                        Label colorMessage = new Label(playerName + " chose " + chosenColor + " color");
                        colorMessage.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-background-color: rgba(0,0,0,0.7); -fx-padding: 10px;");
                        colorMessage.setTranslateY(-50);
                        centerPileBox.getChildren().add(colorMessage);
                        
                        Timeline messageTimeline = new Timeline();
                        messageTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(2), event -> {
                            centerPileBox.getChildren().remove(colorMessage);
                        }));
                        messageTimeline.play();
                    }
                    
                    Pane handBox = (playerIndex == 1) ? opponentHandBoxLeft : 
                                 (playerIndex == 2) ? playerHandBoxTop : opponentHandBoxRight;
                    handBox.getChildren().clear();
                    addCardsToHand(gameState.getPlayerHand(playerIndex), handBox);
                
                    addCardToCenterPile(chosenCard);
                    
                    updateCurrentColorLabel();

                    Timeline nextTurnTimeline = new Timeline();
                    nextTurnTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1.5), event -> {
                        int nextPlayerIndex = gameState.getCurrentPlayerIndex();
                        if (nextPlayerIndex == 0) {
                            isPlayerTurn = true;
                            if (turnIndicatorController != null) {
                                turnIndicatorController.setCurrentPlayer("Your Turn");
                            }
                            System.out.println("AI turns completed, returning control to player");
                        } else {
                            playNextAITurn(nextPlayerIndex);
                        }
                    }));
                    nextTurnTimeline.play();
                }));
                playCardTimeline.play();
            } else {
                System.out.println(playerName + " has no playable cards, drawing one card");
                Timeline drawCardTimeline = new Timeline();
                drawCardTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1.0), ev -> {
                    CardData drawnCard = gameState.drawCard();
                    if (drawnCard != null) {
                        System.out.println(playerName + " drew: " + drawnCard.value + " of " + drawnCard.color);
                        gameState.getPlayerHand(playerIndex).add(drawnCard);
                        Pane handBox = (playerIndex == 1) ? opponentHandBoxLeft : 
                                     (playerIndex == 2) ? playerHandBoxTop : opponentHandBoxRight;
                        handBox.getChildren().clear();
                        addCardsToHand(gameState.getPlayerHand(playerIndex), handBox);
                        boolean canPlay = gameState.canPlayCard(drawnCard);
                        System.out.println(playerName + " can play drawn card: " + canPlay);
                        
                        if (canPlay) {
                            String chosenColor = drawnCard.value.equals("Wild") || drawnCard.value.equals("Wild Draw Four") 
                                ? aiPlayer.chooseColor(gameState.getPlayerHand(playerIndex))
                                : drawnCard.color;
                            System.out.println(playerName + " chose color: " + chosenColor);
                            gameState.playCard(playerIndex, drawnCard, chosenColor);
                            
                            handBox.getChildren().clear();
                            addCardsToHand(gameState.getPlayerHand(playerIndex), handBox);
                            
                            addCardToCenterPile(drawnCard);
                            
                            updateCurrentColorLabel();
                        } else {
                            gameState.moveToNextPlayer();
                        }
                    }
                    
                    Timeline nextTurnTimeline = new Timeline();
                    nextTurnTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1.5), event -> {
                        int nextPlayerIndex = gameState.getCurrentPlayerIndex();
                        if (nextPlayerIndex == 0) {
                            isPlayerTurn = true;
                            if (turnIndicatorController != null) {
                                turnIndicatorController.setCurrentPlayer("Your Turn");
                            }
                            System.out.println("AI turns completed, returning control to player");
                        } else {
                            playNextAITurn(nextPlayerIndex);
                        }
                    }));
                    nextTurnTimeline.play();
                }));
                drawCardTimeline.play();
            }
        }));
        
        timeline.play();
    }

    private void showGameOver(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleDrawCard() {
        if (!isPlayerTurn) return;
        
        if (gameState.getDrawStack() > 0) {
            CardData topCard = gameState.getTopCard();
            
            if (topCard.value.equals("Wild Draw Four")) {
                gameState.drawCards(0);
                playerHandBoxBottom.getChildren().clear();
                addCardsToHand(gameState.getPlayerHand(0), playerHandBoxBottom);
                
                isPlayerTurn = false;
                playAITurns();
                return;
            }
            
            if (topCard.value.equals("Draw Two")) {
                boolean hasDrawCard = false;
                for (CardData card : gameState.getPlayerHand(0)) {
                    if (card.value.equals("Draw Two")) {
                        hasDrawCard = true;
                        break;
                    }
                }
                
                if (!hasDrawCard) {
                    gameState.drawCards(0);
                    playerHandBoxBottom.getChildren().clear();
                    addCardsToHand(gameState.getPlayerHand(0), playerHandBoxBottom);
                    
                    isPlayerTurn = false;
                    playAITurns();
                }
            }
        } else {
            boolean hasPlayableCard = false;
            for (CardData card : gameState.getPlayerHand(0)) {
                if (gameState.canPlayCard(card)) {
                    hasPlayableCard = true;
                    break;
                }
            }
            
            if (hasPlayableCard) {
                return;
            }
            
            CardData drawnCard = gameState.drawCard();
            if (drawnCard != null) {
                gameState.getPlayerHand(0).add(drawnCard);
                
                StackPane cardPane = createCardPane(drawnCard);
                cardPane.getStyleClass().add("bottom-deck-card");
                playerHandBoxBottom.getChildren().add(cardPane);
                
                if (gameState.canPlayCard(drawnCard)) {
                    return;
                }
                
                gameState.moveToNextPlayer();
            }
            
            isPlayerTurn = false;
            playAITurns();
        }
    }

    private void addCardsToHand(List<CardData> cards, Pane handBox) {
        System.out.println("Adding " + cards.size() + " cards to hand");
        for (CardData card : cards) {
            StackPane cardPane = createCardPane(card);
            
            if (handBox == playerHandBoxBottom) {
                cardPane.getStyleClass().add("bottom-deck-card");
            }
            
            handBox.getChildren().add(cardPane);
        }
        System.out.println("Hand now has " + handBox.getChildren().size() + " cards");
    }

    private StackPane createCardPane(CardData card) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/Card.fxml"));
            StackPane cardPane = loader.load();
            CardController cardController = loader.getController();
            cardController.setCard(card.value, card.color);
            
            cardPane.setUserData(cardController);
            
            cardPane.setOnMouseClicked(event -> {
                if (cardPane.getParent() == playerHandBoxBottom) {
                    handleCardClick(card);
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
        
        StackPane cardPane = createCardPane(card);
        centerPileBox.getChildren().add(cardPane);
    }

    private void addCardToHand(CardData card, Pane handBox) {
        StackPane cardPane = createCardPane(card);
        if (handBox == playerHandBoxBottom) {
            cardPane.getStyleClass().add("bottom-deck-card");
        }
        handBox.getChildren().add(cardPane);
    }

    @FXML
    private void handleColorSelection(javafx.event.ActionEvent event) {
        Button button = (Button) event.getSource();
        String color = (String) button.getUserData();
        
        gameState.setCurrentColor(color);
        
        updateCurrentColorLabel();
        
        colorSelectionBox.setVisible(false);
        
        isPlayerTurn = false;
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

    public void setPlayerName(String name) {
        if (turnIndicatorController != null) {
            turnIndicatorController.setPlayerName(name);
        }
    }
} 