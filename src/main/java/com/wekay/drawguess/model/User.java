package com.wekay.drawguess.model;

import lombok.Data;

/**
 * @author Ouerghi Yassine
 */
@Data
public class User {
    private String username;
    private String sessionId;
    private int score;
    private int currentTurnScore;
    private boolean ready;
    private boolean drawing;
    private boolean drew;

    public User(String username, String sessionId) {
        this(username, sessionId, 0);
    }

    public User(String username, String sessionId, int score) {
        this(username, sessionId, score, false, false);
    }

    public User(String username, String sessionId, int score, boolean ready, boolean drawing) {
        this.username = username;
        this.sessionId = sessionId;
        this.score = score;
        this.ready = ready;
        this.currentTurnScore = 0;
        this.drawing = drawing;
    }

    public void addScore(int score) {
        this.score += score;
    }

}
