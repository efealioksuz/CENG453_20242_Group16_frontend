package com.example.unofrontend.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.example.unofrontend.models.CardData;

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

    private static final String[] COLORS = {"red", "yellow", "green", "blue"};
    private static final String[] VALUES = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "Skip", "Reverse", "Draw Two"};
    private static final String[] WILD_VALUES = {"Wild", "Wild Draw Four"};

    @FXML
    public void initialize() {
        List<CardData> deck = createDeck();
        Collections.shuffle(deck);

        playerHandBoxTop.getChildren().clear();
        playerHandBoxBottom.getChildren().clear();
        opponentHandBoxLeft.getChildren().clear();
        opponentHandBoxRight.getChildren().clear();
        centerPileBox.getChildren().clear();

        List<CardData> player1 = new ArrayList<>();
        List<CardData> player2 = new ArrayList<>();
        List<CardData> player3 = new ArrayList<>();
        List<CardData> player4 = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            player1.add(deck.remove(0));
            player2.add(deck.remove(0));
            player3.add(deck.remove(0));
            player4.add(deck.remove(0));
        }

        System.out.println("Player 1 cards: " + player1.size());
        System.out.println("Player 2 cards: " + player2.size());
        System.out.println("Player 3 cards: " + player3.size());
        System.out.println("Player 4 cards: " + player4.size());

        addCardsToHand(playerHandBoxTop, player1);
        addCardsToHand(opponentHandBoxLeft, player2);
        addCardsToHand(opponentHandBoxRight, player3);
        addCardsToHand(playerHandBoxBottom, player4);

        StackPane closedDeck = createClosedCard();
        centerPileBox.getChildren().add(closedDeck);
        CardData openCard = drawValidOpenCard(deck);
        centerPileBox.getChildren().add(createCardPane(openCard));
    }

    public void initializeSinglePlayer() {
        List<CardData> deck = createDeck();
        Collections.shuffle(deck);

        playerHandBoxTop.getChildren().clear();
        playerHandBoxBottom.getChildren().clear();
        opponentHandBoxLeft.getChildren().clear();
        opponentHandBoxRight.getChildren().clear();
        centerPileBox.getChildren().clear();

        List<CardData> playerHand = new ArrayList<>();
        List<CardData> aiHand1 = new ArrayList<>();
        List<CardData> aiHand2 = new ArrayList<>();
        List<CardData> aiHand3 = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            playerHand.add(deck.remove(0));
            aiHand1.add(deck.remove(0));
            aiHand2.add(deck.remove(0));
            aiHand3.add(deck.remove(0));
        }

        System.out.println("Player hand cards: " + playerHand.size());
        System.out.println("AI 1 cards: " + aiHand1.size());
        System.out.println("AI 2 cards: " + aiHand2.size());
        System.out.println("AI 3 cards: " + aiHand3.size());

        addCardsToHand(playerHandBoxBottom, playerHand);
        addCardsToHand(playerHandBoxTop, aiHand1);
        addCardsToHand(opponentHandBoxLeft, aiHand2);
        addCardsToHand(opponentHandBoxRight, aiHand3);  

        StackPane closedDeck = createClosedCard();
        centerPileBox.getChildren().add(closedDeck);
        CardData openCard = drawValidOpenCard(deck);
        centerPileBox.getChildren().add(createCardPane(openCard));
    }

    private void addCardsToHand(Pane handBox, List<CardData> cards) {
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
} 