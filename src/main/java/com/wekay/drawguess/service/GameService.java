package com.wekay.drawguess.service;

import com.wekay.drawguess.model.*;
import com.wekay.drawguess.session.GameSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Ouerghi Yassine
 */
@Service
@AllArgsConstructor
@Slf4j
public class GameService {

    private final static int GAME_START_COUNTDOWN = 5;
    private final static int TURN_TIME = 99;
    private final static int MAX_ROUNDS = 2;
    private final static int MILLISECONDS_BETWEEN_TURN = 5500;
    public static final int MAX_SCORE = 60;

    @Qualifier("gameSession")
    private final GameSession gameSession;
    private final WordService wordService;

    private final SimpMessageSendingOperations simpMessageSendingOperations;

    public void addUser(String username, String sessionId) {
        User user = new User(username, sessionId);
        gameSession.getUsers().add(user);

        if (gameSession.isGameStarted()) {
            user.setReady(true);
            gameSession.getQueue().offer(user);
        }

        publishUsers();
    }

    public void removeUser(String username) {
        gameSession.getUsers().removeIf(user -> username.equalsIgnoreCase(user.getUsername()));
        gameSession.getQueue().removeIf(user -> username.equalsIgnoreCase(user.getUsername()));

        publishUsers();

        if (gameSession.getUsers().size() < 2 && gameSession.isGameStarted()) {
            gameSession.setGameStarted(false);
            gameSession.getQueue().clear();
            stopTurn();
            endGame();
        }
    }

    public void readyUpUser(String username) {
        int readyUsersCount = 0;

        for (User user : gameSession.getUsers()) {
            if (username.equalsIgnoreCase(user.getUsername())) {
                user.setReady(true);
                publishUsers();
            }

            if (user.isReady()) {
                readyUsersCount++;
            }
        }

        //At least 2 players
        if (gameSession.getUsers().size() > 1
                && readyUsersCount == gameSession.getUsers().size()
                && !gameSession.isGameStarted()) {
            startGame();
        }
    }

    public void setWord(String word) {
        gameSession.setCurrentWord(word);
        startTurn();
    }

    public boolean compareWord(String username, String word) {

        if (gameSession.getCurrentPainter() != null
                && gameSession.getCurrentWord() != null
                && !username.equalsIgnoreCase(gameSession.getCurrentPainter().getUsername())
                && gameSession.getCurrentWord().equalsIgnoreCase(word)) {

            int currentTime = gameSession.getCurrentTurnTime();
            final int score;

            if (currentTime >= MAX_SCORE) {
                score = MAX_SCORE;
                gameSession.setCurrentTurnTime(MAX_SCORE);
            } else {
                score = currentTime;
            }

            getUser(username).ifPresent(user -> {
                user.addScore(score);
                user.setCurrentTurnScore(score);
                informWordFound(user, word);
                publishInfoMessage("", username, MessageType.FOUND);
            });

            publishUsers();

            long usersFoundWord = getSortedUsersByScore().stream()
                    .filter(user -> !user.getUsername().equalsIgnoreCase(gameSession.getCurrentPainter().getUsername()))
                    .filter(user -> user.getCurrentTurnScore() > 0)
                    .count();

            if (gameSession.getUsers().size() - 1 == usersFoundWord) {
                stopTurn();
            }

            return true;
        }

        return false;
    }

    private void stopTurn() {
        if (gameSession.getTurnTimer() != null) {
            gameSession.getTurnTimer().cancel();
            gameSession.getTurnTimer().purge();
        }

        publishEndTurn(gameSession.getCurrentWord());

        gameSession.getUsers().forEach(user -> {
            user.setCurrentTurnScore(0);
            user.setDrawing(false);
        });

        if (gameSession.getCurrentPainter() != null) {
            gameSession.getCurrentPainter().setDrew(true);
        }

        publishUsers();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                gameSession.setCurrentPainter(null);
                gameSession.setCurrentWord("");
                gameSession.setTurnTimer(null);
                gameSession.setCurrentTurnTime(0);

                if (isRoundEnded()) {
                    publishEndRound();
                    startRound();
                } else {
                    startPreTurn();
                }
            }
        }, MILLISECONDS_BETWEEN_TURN);
    }

    private void startGame() {
        resetUsers();

        gameSession.setGameStarted(true);
        gameSession.setCurrentRound(0);
        final AtomicInteger gameCountdown = new AtomicInteger(GAME_START_COUNTDOWN);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int seconds = gameCountdown.decrementAndGet();
                publishGameStartCountdown(seconds);
                if (seconds <= 0) {
                    timer.cancel();
                    timer.purge();

                    gameSession.getQueue().clear();
                    startRound();
                }
            }
        }, 500, 1000);
    }

    private void endGame() {
        publishEndGame();
        gameSession.setGameStarted(false);
        gameSession.getUsers().forEach(user -> user.setReady(false));
        publishUsers();
    }

    private void startPreTurn() {
        User user = gameSession.getQueue().poll();
        if (user != null) {
            user.setDrawing(true);
            gameSession.setCurrentPainter(user);
            informDesignatedUser(user, wordService.generateWords());
            publishUsers();
        }
    }

    private void startRound() {
        if (gameSession.incrementAndGetCurrentRound() > MAX_ROUNDS) {
            endGame();
            return;
        }

        gameSession.getUsers().forEach(user -> user.setDrew(false));

        List<User> shuffledUsersList = new ArrayList<>(gameSession.getUsers());
        Collections.shuffle(shuffledUsersList);
        gameSession.getQueue().addAll(shuffledUsersList);

        publishStartRound();

        startPreTurn();
    }

    private void startTurn() {
        if (!gameSession.isGameStarted()) {
            return;
        }

        gameSession.setCurrentTurnTime(TURN_TIME);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int seconds = gameSession.decrementAndGetCurrentTurnTime();

                if (seconds == MAX_SCORE && !usersFoundWord()) {
                    publishAidedWord();
                }

                publishTurnTime(seconds);
                if (seconds <= 0 || !gameSession.isGameStarted()) {
                    stopTurn();
                }
            }
        }, 500, 1000);

        gameSession.setTurnTimer(timer);
    }

    private void publishAidedWord() {
        String currentWord = gameSession.getCurrentWord();
        int letterIndex = new Random().nextInt(currentWord.length());

        StringBuilder stringBuilder = new StringBuilder("_".repeat(currentWord.length()));
        stringBuilder.setCharAt(letterIndex, currentWord.charAt(letterIndex));

        simpMessageSendingOperations.convertAndSend("/topic/word", new GuessedWord(stringBuilder.toString()));
    }

    private boolean isRoundEnded() {
        return gameSession.getQueue().isEmpty();
    }

    private SimpMessageHeaderAccessor createSimpMessageHeader(String sessionId) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);
        return headerAccessor;
    }

    // Broadcast publisher
    private void publishGameStartCountdown(int seconds) {
        String message;
        if (seconds != 0) {
            message = "La3ba bech tibda fi " + seconds + " seconds...";
        } else {
            message = "Bdet!!! GL HF.";
        }

        publishInfoMessage(message, "", MessageType.STARTING);
    }

    private void publishDesignatedUser(String username) {
        publishInfoMessage("", username, MessageType.DRAWING);
    }

    private void publishInfoMessage(String message, String username, MessageType messageType) {
        simpMessageSendingOperations.convertAndSend("/topic/messages", new Message(username, message, messageType));
    }

    private void publishTurnTime(int seconds) {
        simpMessageSendingOperations.convertAndSend("/topic/time", new TurnTime(seconds));
    }

    private void publishUsers() {
        simpMessageSendingOperations.convertAndSend("/topic/users", getSortedUsersByScore());
    }

    private void publishEndTurn(String word) {
        List<UserStat> userStats = getSortedUsersByCurrentScore()
                .stream()
                .map(user -> new UserStat(user.getUsername(), user.getCurrentTurnScore()))
                .collect(Collectors.toList());

        simpMessageSendingOperations.convertAndSend("/topic/end-turn", new EndTurn(true, word, userStats));
    }

    private void publishStartRound() {
        simpMessageSendingOperations.convertAndSend("/topic/start-round", new StartRound(gameSession.getCurrentRound(), MAX_ROUNDS));
    }

    private void publishEndRound() {
        simpMessageSendingOperations.convertAndSend("/topic/end-round", new EndRound(gameSession.getCurrentRound(), MAX_ROUNDS));
    }

    private void publishEndGame() {
        List<UserStat> userStats = getSortedUsersByScore()
                .stream()
                .map(user -> new UserStat(user.getUsername(), user.getScore()))
                .collect(Collectors.toList());

        simpMessageSendingOperations.convertAndSend("/topic/end-game", new EndGame(userStats));
    }

    // Direct user informer
    private void informWordFound(User user, String word) {
        SimpMessageHeaderAccessor headerAccessor = createSimpMessageHeader(user.getSessionId());

        simpMessageSendingOperations.convertAndSendToUser(
                user.getSessionId(),
                "/topic/word-found",
                new GuessedWord(word),
                headerAccessor.getMessageHeaders()
        );
    }

    private void informDesignatedUser(User designatedUser, List<String> words) {
        SimpMessageHeaderAccessor headerAccessor = createSimpMessageHeader(designatedUser.getSessionId());

        simpMessageSendingOperations.convertAndSendToUser(
                designatedUser.getSessionId(),
                "/topic/words-suggestion",
                new WordChoice(words),
                headerAccessor.getMessageHeaders()
        );

        publishDesignatedUser(designatedUser.getUsername());
    }

    // Users
    private List<User> getSortedUsersByScore() {
        gameSession.getUsers().sort((user1, user2) -> user2.getScore() - user1.getScore());
        return gameSession.getUsers();
    }

    private List<User> getSortedUsersByCurrentScore() {
        gameSession.getUsers().sort((user1, user2) -> user2.getCurrentTurnScore() - user1.getCurrentTurnScore());
        return gameSession.getUsers();
    }

    private void resetUsers() {
        gameSession.getUsers().forEach(user -> {
            user.setScore(0);
            user.setCurrentTurnScore(0);
            user.setCurrentTurnScore(0);
            user.setDrawing(false);
            user.setDrew(false);
        });
    }

    public Optional<User> getUser(String username) {
        return gameSession.getUsers().stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    private boolean usersFoundWord() {
        return gameSession.getUsers().stream()
                .anyMatch(user -> user.getCurrentTurnScore() != 0);
    }
}
