package com.example.unofrontend.models;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class LeaderboardEntry {
    private final SimpleIntegerProperty rank;
    private final SimpleStringProperty username;
    private final SimpleDoubleProperty score;

    public LeaderboardEntry(int rank, String username, double score) {
        this.rank = new SimpleIntegerProperty(rank);
        this.username = new SimpleStringProperty(username);
        this.score = new SimpleDoubleProperty(score);
    }

    public int getRank() {
        return rank.get();
    }

    public String getUsername() {
        return username.get();
    }

    public double getScore() {
        return score.get();
    }

    public SimpleIntegerProperty rankProperty() {
        return rank;
    }

    public SimpleStringProperty usernameProperty() {
        return username;
    }

    public SimpleDoubleProperty scoreProperty() {
        return score;
    }
} 