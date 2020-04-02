package com.wekay.drawguess.socket;

import com.wekay.drawguess.model.*;
import com.wekay.drawguess.service.GameService;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * @author Ouerghi Yassine
 */
@Controller
@AllArgsConstructor
public class SocketController {

    public final static String USERNAME_KEY = "username";

    private final GameService gameService;

    @MessageMapping("/connect")
    @SendTo("/topic/messages")
    public Message connect(Connect connect, SimpMessageHeaderAccessor simpMessageHeaderAccessor) {
        simpMessageHeaderAccessor.getSessionAttributes().put("username", connect.getUsername());

        gameService.addUser(connect.getUsername(), simpMessageHeaderAccessor.getSessionId());

        return new Message(connect.getUsername(), "", MessageType.CONNECT);
    }

    @MessageMapping("/ready")
    @SendTo("/topic/messages")
    public Message ready(SimpMessageHeaderAccessor simpMessageHeaderAccessor) {
        String username = getUsername(simpMessageHeaderAccessor);

        gameService.readyUpUser(getUsername(simpMessageHeaderAccessor));

        return new Message(username, "", MessageType.READY);
    }

    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public Message send(MessageInput messageInput, SimpMessageHeaderAccessor simpMessageHeaderAccessor) {
        String username = getUsername(simpMessageHeaderAccessor);
        boolean found = gameService.compareWord(username, messageInput.getText());

        if (!found) {
            return new Message(username, messageInput.getText(), MessageType.MESSAGE);
        }

        return null;
    }

    @MessageMapping("/draw")
    @SendTo("/topic/draw")
    public Draw draw(Draw draw, SimpMessageHeaderAccessor simpMessageHeaderAccessor) {
        draw.setUsername(getUsername(simpMessageHeaderAccessor));

        return draw;
    }

    @MessageMapping("/cursor")
    @SendTo("/topic/cursor")
    public Cursor cursor(Cursor cursor, SimpMessageHeaderAccessor simpMessageHeaderAccessor) {
        cursor.setUsername(getUsername(simpMessageHeaderAccessor));

        return cursor;
    }

    @MessageMapping("/clear")
    @SendTo("/topic/clear")
    public Clear clear(Clear clear, SimpMessageHeaderAccessor simpMessageHeaderAccessor) {
        clear.setUsername(getUsername(simpMessageHeaderAccessor));

        return clear;
    }

    @MessageMapping("/word")
    @SendTo("/topic/word")
    public GuessedWord word(Word word, SimpMessageHeaderAccessor simpMessageHeaderAccessor) {
        gameService.setWord(word.getWord());

        String hiddenWord = "_".repeat(word.getWord().length());

        return new GuessedWord(hiddenWord);
    }


    private String getUsername(SimpMessageHeaderAccessor simpMessageHeaderAccessor) {
        return simpMessageHeaderAccessor.getSessionAttributes().get(USERNAME_KEY).toString();
    }
}