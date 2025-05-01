package com.example.unofrontend.models;

public class Leaderboard {
    private String username;
    private Integer totalScore;

    
    public Leaderboard() {
    }

    public Leaderboard(String username, Integer totalScore) {
        this.username = username;
        this.totalScore = totalScore;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }
} 