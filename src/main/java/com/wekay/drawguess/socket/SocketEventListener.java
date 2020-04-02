package com.wekay.drawguess.socket;

import com.wekay.drawguess.model.MessageType;
import com.wekay.drawguess.model.Message;
import com.wekay.drawguess.service.GameService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import static com.wekay.drawguess.socket.SocketController.USERNAME_KEY;

/**
 * @author Ouerghi Yassine
 */
@Component
@AllArgsConstructor
@Slf4j
public class SocketEventListener {

    private final SimpMessageSendingOperations simpMessageSendingOperations;
    private final GameService gameService;

    @EventListener
    public void handleSocketDisconnectListener(final SessionDisconnectEvent sessionDisconnectEvent) {

        final StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(sessionDisconnectEvent.getMessage());

        if (headerAccessor.getSessionAttributes() != null && headerAccessor.getSessionAttributes().containsKey(USERNAME_KEY)) {
            String username = headerAccessor.getSessionAttributes().get(USERNAME_KEY).toString();
            gameService.removeUser(username);

            log.info("Removed user '{}' from game session", username);

            Message message = new Message(username, "", MessageType.DISCONNECT);
            simpMessageSendingOperations.convertAndSend("/topic/messages", message);
        }
    }
}
