package com.example.unofrontend.controllers;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.springframework.stereotype.Component;

@Component
public class DirectionIndicatorController {
    @FXML
    private ImageView directionImage;
    
    private Image clockwiseImage;
    private Image counterClockwiseImage;
    
    public DirectionIndicatorController() {
        try {
            this.clockwiseImage = new Image(getClass().getResourceAsStream("/images/rotate-right.png"));
            this.counterClockwiseImage = new Image(getClass().getResourceAsStream("/images/rotate-left.png"));
            System.out.println("Direction indicator images loaded: " + 
                              (clockwiseImage != null ? "clockwise OK" : "clockwise NULL") + ", " +
                              (counterClockwiseImage != null ? "counter-clockwise OK" : "counter-clockwise NULL"));
        } catch (Exception e) {
            System.err.println("Error loading direction images: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    public void initialize() {
        try {
            if (directionImage != null) {
                directionImage.setImage(clockwiseImage);
                directionImage.setOpacity(1.0);
                directionImage.setVisible(true);
                System.out.println("Direction indicator initialized successfully");
            } else {
                System.err.println("directionImage is NULL in initialize");
            }
        } catch (Exception e) {
            System.err.println("Error in direction indicator initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setClockwise(boolean isClockwise) {
        try {
            if (directionImage != null) {
                directionImage.setImage(isClockwise ? clockwiseImage : counterClockwiseImage);
                directionImage.setOpacity(1.0);
                directionImage.setVisible(true);
                System.out.println("Direction set to: " + (isClockwise ? "clockwise" : "counter-clockwise"));
            } else {
                System.err.println("directionImage is NULL in setClockwise");
            }
        } catch (Exception e) {
            System.err.println("Error setting direction: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 