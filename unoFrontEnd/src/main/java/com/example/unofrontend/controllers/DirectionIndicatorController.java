package com.example.unofrontend.controllers;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.ParallelTransition;
import javafx.util.Duration;
import org.springframework.stereotype.Component;

@Component
public class DirectionIndicatorController {
    @FXML
    private ImageView directionImage;
    
    private Image clockwiseImage;
    private Image counterClockwiseImage;
    private ParallelTransition pulseAnimation;
    
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
                setupPulseAnimation();
                System.out.println("Direction indicator initialized successfully");
            } else {
                System.err.println("directionImage is NULL in initialize");
            }
        } catch (Exception e) {
            System.err.println("Error in direction indicator initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupPulseAnimation() {
        if (pulseAnimation != null) {
            pulseAnimation.stop();
        }


        ScaleTransition scale = new ScaleTransition(Duration.seconds(0.5), directionImage);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.2);
        scale.setToY(1.2);
        scale.setCycleCount(ScaleTransition.INDEFINITE);
        scale.setAutoReverse(true);


        FadeTransition fade = new FadeTransition(Duration.seconds(0.5), directionImage);
        fade.setFromValue(1.0);
        fade.setToValue(0.7);
        fade.setCycleCount(FadeTransition.INDEFINITE);
        fade.setAutoReverse(true);


        pulseAnimation = new ParallelTransition(scale, fade);
        pulseAnimation.play();
    }

    public void setClockwise(boolean isClockwise) {
        try {
            if (directionImage != null) {

                if (pulseAnimation != null) {
                    pulseAnimation.stop();
                }
                
                directionImage.setImage(isClockwise ? clockwiseImage : counterClockwiseImage);
                directionImage.setOpacity(1.0);
                directionImage.setVisible(true);
                

                directionImage.setScaleX(1.0);
                directionImage.setScaleY(1.0);
                setupPulseAnimation();
                
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