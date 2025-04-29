package com.example.unofrontend.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DirectionIndicatorController {
    @FXML
    private Label directionLabel;

    public void setDirection(String direction) {
        directionLabel.setText("Direction: " + direction);
    }
} 