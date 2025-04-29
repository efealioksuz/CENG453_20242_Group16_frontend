package com.example.unofrontend.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.springframework.stereotype.Component;

@Component
public class TurnIndicatorController {
    @FXML
    private Label playerNameLabel;

    public void setPlayerName(String name) {
        playerNameLabel.setText(name);
    }
} 