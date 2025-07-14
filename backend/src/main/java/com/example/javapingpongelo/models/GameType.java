package com.example.javapingpongelo.models;

import lombok.Getter;

/**
 * Enum representing the different types of games a player can participate in.
 */
@Getter
public enum GameType {
    SINGLES_RANKED("singlesRanked"),
    DOUBLES_RANKED("doublesRanked"),
    SINGLES_NORMAL("singlesNormal"),
    DOUBLES_NORMAL("doublesNormal");

    private final String value;

    GameType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}