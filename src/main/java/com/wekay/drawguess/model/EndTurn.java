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
public class EndTurn {
    private boolean end;
    private String word;
    private String time;
    private List<UserStat> stats;

    public EndTurn(boolean end, String word, List<UserStat> stats) {
        this.end = end;
        this.word = word;
        this.stats = stats;
        this.time = DateUtils.getCurrentTime();
    }
}
