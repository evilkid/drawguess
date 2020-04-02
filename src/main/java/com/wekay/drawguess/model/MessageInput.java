package com.wekay.drawguess.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Ouerghi Yassine
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageInput {
    private String username;
    private String text;
}
