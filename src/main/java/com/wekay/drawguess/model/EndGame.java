package com.wekay.drawguess.model;

import com.wekay.drawguess.utils.DateUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Ouerghi Yassine
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EndGame {
    private List<UserStat> stats;
    private String time;

    public EndGame(List<UserStat> stats) {
        this.stats = stats;
        this.time = DateUtils.getCurrentTime();
    }
}