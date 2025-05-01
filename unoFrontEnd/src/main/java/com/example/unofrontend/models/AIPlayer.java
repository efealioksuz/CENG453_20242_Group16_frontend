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
        List<CardData> matchingCards = new ArrayList<>();
        CardData wildDrawFour = null;

        for (CardData card : hand) {
            if (card.value.equals("Wild Draw Four")) {
                wildDrawFour = card;
            } else if (card.value.equals("Wild") || 
                      card.color.equals(gameState.getCurrentColor()) || 
                      card.value.equals(gameState.getTopCard().value)) {
                matchingCards.add(card);
            }
        }
        
        if (!matchingCards.isEmpty()) {
            return matchingCards.get(random.nextInt(matchingCards.size()));
        }
        
        if (wildDrawFour != null) {
            return wildDrawFour;
        }
        
        return null;
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

    public CardData chooseCardAfterDraw(CardData drawnCard, GameState gameState) {
        if (drawnCard.value.equals("Wild") || 
            drawnCard.value.equals("Wild Draw Four") || 
            drawnCard.color.equals(gameState.getCurrentColor()) || 
            drawnCard.value.equals(gameState.getTopCard().value)) {
            return drawnCard;
        }
        return null;
    }
} 