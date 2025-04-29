package com.example.unofrontend.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.springframework.stereotype.Component;

@Component
public class DirectionIndicatorController {
    @FXML
    private Label directionLabel;

    public void setDirection(String direction) {
        directionLabel.setText(direction);
    }
} 