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

    private static final String[] COLORS = {"red", "yellow", "green", "blue"};
    private static final String[] VALUES = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "Skip", "Reverse", "Draw Two"};
    private static final String[] WILD_VALUES = {"Wild", "Wild Draw Four"};

    private GameState gameState;
    private AIPlayer aiPlayer;
    private boolean isPlayerTurn;

    @FXML
    public void initialize() {
        gameState = new GameState();
        aiPlayer = new AIPlayer();
        isPlayerTurn = true;
        
        // Initialize the game
        gameState.startGame();
        
        // Add cards to hands
        addCardsToHand(gameState.getPlayerHand(0), playerHandBoxBottom);
        addCardsToHand(gameState.getPlayerHand(1), playerHandBoxTop);
        addCardsToHand(gameState.getPlayerHand(2), opponentHandBoxLeft);
        addCardsToHand(gameState.getPlayerHand(3), opponentHandBoxRight);
        
        // Add top card to center pile
        CardData topCard = gameState.getTopCard();
        if (topCard != null) {
            addCardToCenterPile(topCard);
        }
        
        // Update current color label
        updateCurrentColorLabel();
    }

    private void updateCurrentColorLabel() {
        currentColorLabel.setText("Current Color: " + gameState.getCurrentColor());
    }

    private void handleCardClick(CardData card) {
        if (!isPlayerTurn) return;
        
        if (gameState.canPlayCard(card)) {
            // Play the card
            gameState.playCard(0, card, card.color);
            
            // Remove card from player's hand
            playerHandBoxBottom.getChildren().removeIf(node -> {
                if (node instanceof StackPane) {
                    CardController cardController = (CardController) node.getUserData();
                    return cardController != null && cardController.getCardData().equals(card);
                }
                return false;
            });
            
            // Add card to center pile
            addCardToCenterPile(card);
            
            // Update current color
            updateCurrentColorLabel();
            
            // Check if player won
            if (gameState.getPlayerHand(0).isEmpty()) {
                showGameOver("You won!");
                return;
            }
            
            // Move to next player
            isPlayerTurn = false;
            
            // Start AI turns
            playAITurns();
        }
    }

    private void playAITurns() {
        // Play turns for all AI players
        for (int i = 1; i < 4; i++) {
            if (gameState.getPlayerHand(i).isEmpty()) {
                showGameOver("Player " + i + " won!");
                return;
            }
            
            // AI chooses a card to play
            CardData chosenCard = aiPlayer.chooseCardToPlay(gameState.getPlayerHand(i), gameState);
            
            if (chosenCard != null) {
                // Play the card
                String chosenColor = aiPlayer.chooseColor(gameState.getPlayerHand(i));
                gameState.playCard(i, chosenCard, chosenColor);
                
                // Remove card from AI's hand
                Pane handBox = (i == 1) ? playerHandBoxTop : (i == 2) ? opponentHandBoxLeft : opponentHandBoxRight;
                handBox.getChildren().removeIf(node -> {
                    if (node instanceof StackPane) {
                        CardController cardController = (CardController) node.getUserData();
                        return cardController != null && cardController.getCardData().equals(chosenCard);
                    }
                    return false;
                });
                
                // Add card to center pile
                addCardToCenterPile(chosenCard);
                
                // Update current color
                updateCurrentColorLabel();
            } else {
                // AI needs to draw a card
                gameState.drawCards(i);
                
                // Refresh the AI's hand display
                Pane handBox = (i == 1) ? playerHandBoxTop : (i == 2) ? opponentHandBoxLeft : opponentHandBoxRight;
                handBox.getChildren().clear();
                addCardsToHand(gameState.getPlayerHand(i), handBox);
            }
        }
        
        // Return control to player
        isPlayerTurn = true;
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
        
        gameState.drawCards(0);
        
        // Refresh the player's hand display
        playerHandBoxBottom.getChildren().clear();
        addCardsToHand(gameState.getPlayerHand(0), playerHandBoxBottom);
        
        isPlayerTurn = false;
        playAITurns();
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
            
            // Set the userData to the card controller
            cardPane.setUserData(cardController);
            
            // Add click handler for player's cards
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
        StackPane closed = new StackPane();
        closed.setMinWidth(100);
        closed.setMinHeight(150);
        closed.setStyle("-fx-background-color: black; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: black; -fx-border-width: 2;");
        return closed;
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

    public void initializeSinglePlayer() {
        initialize();
    }

    @FXML
    private void handleColorSelection(javafx.event.ActionEvent event) {
        Button button = (Button) event.getSource();
        String color = (String) button.getUserData();
        
        // Update the game state with the chosen color
        gameState.setCurrentColor(color);
        
        // Update the UI
        updateCurrentColorLabel();
        
        // Hide the color selection buttons
        colorSelectionBox.setVisible(false);
        
        // Continue with AI turns
        isPlayerTurn = false;
        playAITurns();
    }

    @FXML
    private void handleUno() {
        if (!isPlayerTurn) return;
        
        // Check if player has exactly one card
        if (gameState.getPlayerHand(0).size() == 1) {
            // Player successfully called UNO
            unoButton.setVisible(false);
        } else {
            // Player called UNO incorrectly
            showGameOver("You called UNO incorrectly! You must draw 2 cards.");
            gameState.drawCards(0);
            gameState.drawCards(0);
            
            // Refresh the player's hand display
            playerHandBoxBottom.getChildren().clear();
            addCardsToHand(gameState.getPlayerHand(0), playerHandBoxBottom);
        }
    }
} 