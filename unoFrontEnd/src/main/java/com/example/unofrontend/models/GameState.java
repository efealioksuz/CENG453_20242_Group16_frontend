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
    private int direction = 1;

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
        
        if (drawStack > 0) {
            if (topCard.value.equals("Draw Two")) {
                return card.value.equals("Draw Two");
            }
            return false;
        }
        
        if (card.value.equals("Wild Draw Four")) {
            List<CardData> playerHand = null;
            for (int i = 0; i < playerHands.size(); i++) {
                if (playerHands.get(i).contains(card)) {
                    playerHand = playerHands.get(i);
                    break;
                }
            }
            if (playerHand != null) {
                for (CardData handCard : playerHand) {
                    if (handCard != card && (handCard.color.equals(currentColor) || handCard.value.equals(topCard.value))) {
                        return false;
                    }
                }
            }
            return true;
        }
        
        if (card.value.equals("Wild")) {
            return true;
        }
        
        return card.color.equals(currentColor) || card.value.equals(topCard.value);
    }

    private boolean hasPlayableCard(int playerIndex) {
        for (CardData card : playerHands.get(playerIndex)) {
            if (isValidPlay(card)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidPlay(CardData card) {
        CardData topCard = discardPile.get(discardPile.size() - 1);
        
        if (drawStack > 0) {
            if (topCard.value.equals("Draw Two")) {
                return card.value.equals("Draw Two");
            }
            return false;
        }
        
        if (card.value.equals("Wild Draw Four")) {
            List<CardData> playerHand = null;
            for (int i = 0; i < playerHands.size(); i++) {
                if (playerHands.get(i).contains(card)) {
                    playerHand = playerHands.get(i);
                    break;
                }
            }
            if (playerHand != null) {
                for (CardData handCard : playerHand) {
                    if (handCard != card && (handCard.color.equals(currentColor) || handCard.value.equals(topCard.value))) {
                        return false;
                    }
                }
            }
            return true;
        }
        
        if (card.value.equals("Wild")) {
            return true;
        }
        
        return card.color.equals(currentColor) || card.value.equals(topCard.value);
    }

    public void playCard(int playerIndex, CardData card, String chosenColor) {
        if (playerIndex != currentPlayerIndex) {
            throw new IllegalStateException("Not your turn");
        }

        if (!isValidPlay(card)) {
            throw new IllegalStateException("Invalid play");
        }

        playerHands.get(playerIndex).remove(card);

        discardPile.add(card);
        if (card.value.equals("Wild") || card.value.equals("Wild Draw Four")) {
            currentColor = chosenColor;
        } else {
            currentColor = card.color;
        }

        if (card.value.equals("Skip")) {
            currentPlayerIndex = (currentPlayerIndex + 2) % 4;
        } else if (card.value.equals("Reverse")) {
            direction *= -1;
            currentPlayerIndex = (currentPlayerIndex + direction + 4) % 4;
        } else if (card.value.equals("Draw Two")) {
            drawStack += 2;
            currentPlayerIndex = (currentPlayerIndex + direction + 4) % 4;
        } else if (card.value.equals("Wild Draw Four")) {
            drawStack = 4;
            currentPlayerIndex = (currentPlayerIndex + direction + 4) % 4;
        } else {
            currentPlayerIndex = (currentPlayerIndex + direction + 4) % 4;
        }
    }

    public void drawCards(int playerIndex) {
        if (drawStack > 0) {
            int cardsToDraw = drawStack;
            drawStack = 0;
            
            for (int i = 0; i < cardsToDraw; i++) {
                CardData card = drawCard();
                if (card != null) {
                    playerHands.get(playerIndex).add(card);
                }
            }
            
            currentPlayerIndex = (currentPlayerIndex + direction + 4) % 4;
        } else {
            CardData card = drawCard();
            if (card != null) {
                playerHands.get(playerIndex).add(card);
            }
        }
    }

    public void moveToNextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + direction + 4) % 4;
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