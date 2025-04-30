package com.example.unofrontend.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AIPlayer {
    private Random random;
    private String[] colors = {"red", "yellow", "green", "blue"};

    public AIPlayer() {
        this.random = new Random();
    }

    public CardData chooseCardToPlay(List<CardData> hand, GameState gameState) {
        List<CardData> playableCards = new ArrayList<>();

        for (CardData card : hand) {
            if (gameState.canPlayCard(card)) {
                playableCards.add(card);
            }
        }
        
        if (playableCards.isEmpty()) {
            return null;
        }
        
        return playableCards.get(random.nextInt(playableCards.size()));
    }

    public String chooseColor(List<CardData> hand) {
        return colors[random.nextInt(colors.length)];
    }
} 