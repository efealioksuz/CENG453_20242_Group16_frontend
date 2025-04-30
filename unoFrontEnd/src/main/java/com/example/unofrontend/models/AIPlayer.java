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
        
        List<CardData> matchingColorCards = new ArrayList<>();
        for (CardData card : playableCards) {
            if (card.color.equals(gameState.getCurrentColor())) {
                matchingColorCards.add(card);
            }
        }
        
        if (!matchingColorCards.isEmpty()) {
            return matchingColorCards.get(random.nextInt(matchingColorCards.size()));
        }
        
        return playableCards.get(random.nextInt(playableCards.size()));
    }

    public String chooseColor(List<CardData> hand) {
        int[] colorCounts = new int[4];
        for (CardData card : hand) {
            switch (card.color) {
                case "red": colorCounts[0]++; break;
                case "yellow": colorCounts[1]++; break;
                case "green": colorCounts[2]++; break;
                case "blue": colorCounts[3]++; break;
            }
        }
        
        int maxCount = -1;
        int maxIndex = 0;
        for (int i = 0; i < colorCounts.length; i++) {
            if (colorCounts[i] > maxCount) {
                maxCount = colorCounts[i];
                maxIndex = i;
            }
        }
        
        return colors[maxIndex];
    }

    public CardData chooseDrawCardToPlay(List<CardData> hand, GameState gameState) {
        if (gameState.getDrawStack() <= 0) {
            return null;
        }

        CardData topCard = gameState.getTopCard();
        
        if (topCard.value.equals("Draw Two")) {
            for (CardData card : hand) {
                if (card.value.equals("Draw Two")) {
                    return card;
                }
            }
        }

        return null;
    }
} 