package com.wekay.drawguess.session;

import com.wekay.drawguess.model.User;
import lombok.Data;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Timer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Ouerghi Yassine
 */
@Data
public class GameSession {
    private CopyOnWriteArrayList<User> users;
    private ConcurrentLinkedQueue<User> queue;
    private User currentPainter;
    private String currentWord;
    private Timer turnTimer;
    private Integer currentTurnTime;
    private Integer currentRound;
    private boolean gameStarted;

    public GameSession() {
        this.users = new CopyOnWriteArrayList<>();
        this.queue = new ConcurrentLinkedQueue<>();
        this.currentPainter = null;
        this.currentWord = null;
        this.turnTimer = null;
        this.currentTurnTime = 0;
        this.currentRound = 0;
        this.gameStarted = false;
    }

    public Integer incrementAndGetCurrentRound() {
        return ++this.currentRound;
    }

    public Integer decrementAndGetCurrentTurnTime() {
        return --this.currentTurnTime;
    }
}
