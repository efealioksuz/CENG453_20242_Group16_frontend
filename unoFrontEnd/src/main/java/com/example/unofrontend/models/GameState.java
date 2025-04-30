package com.example.unofrontend.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameState {
    private List<CardData> deck;
    private List<CardData> discardPile;
    private List<List<CardData>> playerHands;
    private int currentPlayerIndex;
    private boolean clockwise;
    private String currentColor;
    private int drawStack;
    private Random random;

    public GameState() {
        this.deck = new ArrayList<>();
        this.discardPile = new ArrayList<>();
        this.playerHands = new ArrayList<>();
        this.clockwise = true;
        this.drawStack = 0;
        this.random = new Random();
        initializeDeck();
    }

    private void initializeDeck() {
        String[] colors = {"red", "yellow", "green", "blue"};
        String[] values = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "Skip", "Reverse", "Draw Two"};
        
        for (String color : colors) {
            for (String value : values) {
                deck.add(new CardData(value, color));
                deck.add(new CardData(value, color));
            }
        }

        for (int i = 0; i < 8; i++) {
            deck.add(new CardData("Wild", "black"));
            deck.add(new CardData("Wild Draw Four", "black"));
        }
    }

    public void startGame() {
        shuffleDeck();
        
        for (int i = 0; i < 4; i++) {
            List<CardData> hand = new ArrayList<>();
            for (int j = 0; j < 7; j++) {
                hand.add(drawCard());
            }
            playerHands.add(hand);
        }

        CardData firstCard = drawCard();
        while (firstCard.value.equals("Wild") || firstCard.value.equals("Wild Draw Four")) {
            deck.add(firstCard);
            shuffleDeck();
            firstCard = drawCard();
        }
        discardPile.add(firstCard);
        currentColor = firstCard.color;
    }

    private void shuffleDeck() {
        for (int i = deck.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            CardData temp = deck.get(i);
            deck.set(i, deck.get(j));
            deck.set(j, temp);
        }
    }

    public CardData drawCard() {
        if (deck.isEmpty()) {
            CardData topCard = discardPile.remove(discardPile.size() - 1);
            deck.addAll(discardPile);
            discardPile.clear();
            discardPile.add(topCard);
            shuffleDeck();
        }
        return deck.remove(0);
    }

    public boolean canPlayCard(CardData card) {
        CardData topCard = discardPile.get(discardPile.size() - 1);
        
        if (card.value.equals("Wild Draw Four")) {
            return !hasPlayableCard(currentPlayerIndex);
        }
        
        if (card.value.equals("Wild")) {
            return true;
        }
        
        return card.color.equals(currentColor) || card.value.equals(topCard.value);
    }

    private boolean hasPlayableCard(int playerIndex) {
        for (CardData card : playerHands.get(playerIndex)) {
            if (canPlayCard(card)) {
                return true;
            }
        }
        return false;
    }

    public void playCard(int playerIndex, CardData card, String chosenColor) {
        if (!canPlayCard(card)) {
            throw new IllegalStateException("Cannot play this card");
        }

        playerHands.get(playerIndex).remove(card);
        discardPile.add(card);

        switch (card.value) {
            case "Wild":
            case "Wild Draw Four":
                currentColor = chosenColor;
                if (card.value.equals("Wild Draw Four")) {
                    drawStack += 4;
                }
                break;
            case "Reverse":
                clockwise = !clockwise;
                break;
            case "Skip":
                moveToNextPlayer();
                break;
            case "Draw Two":
                drawStack += 2;
                break;
            default:
                currentColor = card.color;
        }

        moveToNextPlayer();
    }

    private void moveToNextPlayer() {
        if (clockwise) {
            currentPlayerIndex = (currentPlayerIndex + 1) % 4;
        } else {
            currentPlayerIndex = (currentPlayerIndex + 3) % 4;
        }
    }

    public void drawCards(int playerIndex) {
        int cardsToDraw = drawStack > 0 ? drawStack : 1;
        
        int maxAvailableCards = deck.size() + (discardPile.size() - 1);
        
        if (maxAvailableCards <= 0) {
            drawStack = 0;
            return;
        }
        
        cardsToDraw = Math.min(cardsToDraw, maxAvailableCards);
        
        for (int i = 0; i < cardsToDraw; i++) {
            playerHands.get(playerIndex).add(drawCard());
        }
        
        drawStack = 0;
    }

    public CardData getTopCard() {
        return discardPile.get(discardPile.size() - 1);
    }

    public String getCurrentColor() {
        return currentColor;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public List<CardData> getPlayerHand(int playerIndex) {
        return playerHands.get(playerIndex);
    }

    public boolean isClockwise() {
        return clockwise;
    }

    public int getDrawStack() {
        return drawStack;
    }

    public void setCurrentColor(String color) {
        this.currentColor = color;
    }
} 