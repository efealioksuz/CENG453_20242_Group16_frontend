package com.example.unofrontend.models;

public class CardData {
    public String value;
    public String color;

    public CardData(String value, String color) {
        this.value = value;
        this.color = color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardData cardData = (CardData) o;
        return value.equals(cardData.value) && color.equals(cardData.color);
    }

    @Override
    public int hashCode() {
        return value.hashCode() * 31 + color.hashCode();
    }
} 