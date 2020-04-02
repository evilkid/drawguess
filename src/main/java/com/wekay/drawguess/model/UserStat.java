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
public class UserStat {
    private String username;
    private int score;
}
