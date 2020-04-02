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
public class EndRound {
    private int currentRound;
    private int maximumRound;
    private String time;

    public EndRound(int currentRound, int maximumRound) {
        this.currentRound = currentRound;
        this.maximumRound = maximumRound;
        this.time = DateUtils.getCurrentTime();
    }
}
