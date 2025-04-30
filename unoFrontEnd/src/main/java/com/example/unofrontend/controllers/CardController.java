package com.example.unofrontend.controllers;

import com.example.unofrontend.models.CardData;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class CardController {
    @FXML
    private StackPane cardPane;
    @FXML
    private Label topLeftNumber;
    @FXML
    private Label bottomRightNumber;
    @FXML
    private Label cardSymbol;
    @FXML
    private Label unoText;

    private CardData cardData;

    public void setCard(String value, String color) {
        this.cardData = new CardData(value, color);
        
        cardPane.getStyleClass().removeAll(
            "uno-card-red", "uno-card-blue", "uno-card-green", 
            "uno-card-yellow", "uno-card-black"
        );
        
        cardPane.getStyleClass().add("uno-card-" + color.toLowerCase());
        
        if (isActionCard(value)) {
            topLeftNumber.getStyleClass().add("card-special");
            bottomRightNumber.getStyleClass().add("card-special");
            setActionSymbol(value);
        } else {
            topLeftNumber.getStyleClass().remove("card-special");
            bottomRightNumber.getStyleClass().remove("card-special");
            setCornerNumbers(value);
            cardSymbol.setText(value);
        }

        unoText.setVisible(false);
        cardSymbol.setVisible(true);
        topLeftNumber.setVisible(true);
        bottomRightNumber.setVisible(true);
    }

    public CardData getCardData() {
        return cardData;
    }

    private void setCornerNumbers(String value) {
        topLeftNumber.setText(value);
        bottomRightNumber.setText(value);
    }

    private boolean isActionCard(String value) {
        return value.equals("Skip") || value.equals("Reverse") || 
               value.equals("Draw Two") || value.equals("Wild") || 
               value.equals("Wild Draw Four");
    }

    private void setActionSymbol(String value) {
        String symbol;
        switch (value) {
            case "Skip" -> symbol = "⊘";
            case "Reverse" -> symbol = "↺";
            case "Draw Two" -> symbol = "+2";
            case "Wild" -> symbol = "★";
            case "Wild Draw Four" -> symbol = "+4";
            default -> symbol = value;
        }
        
        cardSymbol.setText(symbol);
        topLeftNumber.setText(symbol);
        bottomRightNumber.setText(symbol);
    }

    public void setCardValue(String value) {
        if (isActionCard(value)) {
            setActionSymbol(value);
        } else {
            setCornerNumbers(value);
            cardSymbol.setText(value);
        }
    }

    public void setSelected(boolean selected) {
        if (selected) {
            cardPane.getStyleClass().add("selected");
        } else {
            cardPane.getStyleClass().remove("selected");
        }
    }

    public void setCardBack() {
        cardSymbol.setVisible(false);
        topLeftNumber.setVisible(false);
        bottomRightNumber.setVisible(false);
        
        unoText.setVisible(true);
        
        cardPane.getStyleClass().removeAll(
            "uno-card-red", "uno-card-blue", "uno-card-green", 
            "uno-card-yellow", "uno-card-black"
        );
        cardPane.getStyleClass().add("uno-card-back");
    }
} 