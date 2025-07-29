package com.example.javapingpongelo.models;

public enum ChallengeStatus {
    PENDING,     // Challenge sent, waiting for response
    ACCEPTED,    // Challenge accepted, game should be played
    DECLINED,    // Challenge declined
    EXPIRED,     // Challenge expired without response
    COMPLETED,   // Game played and logged
    CANCELLED    // Challenge cancelled by challenger
}