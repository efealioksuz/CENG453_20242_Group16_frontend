package com.example.unofrontend.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class CardController {
    @FXML
    private StackPane cardPane;
    @FXML
    private Label cardValue;

    public void setCard(String value, String color) {
        cardValue.setText(value);
        setCardColor(color);
    }

    public void setCardColor(String color) {
        String fxColor = color;
     
        if (color.equalsIgnoreCase("wild") || color.equalsIgnoreCase("black")) {
            fxColor = "black";
        }
        cardPane.setStyle("-fx-background-color: " + fxColor + ";" +
                "-fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: black; -fx-border-width: 2;");
    }

    public void setCardValue(String value) {
        cardValue.setText(value);
    }
} 