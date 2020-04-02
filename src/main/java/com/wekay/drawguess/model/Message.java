package com.wekay.drawguess.model;

import com.wekay.drawguess.utils.DateUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Ouerghi Yassine
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    private String username;
    private String text;
    private String time;
    private MessageType type;

    public Message(String username, String text, MessageType type) {
        this.username = username;
        this.text = text;
        this.type = type;
        this.time = DateUtils.getCurrentTime();
    }
}
