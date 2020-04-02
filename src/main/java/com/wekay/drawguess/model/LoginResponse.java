package com.wekay.drawguess.model;

import lombok.Data;

/**
 * @author Ouerghi Yassine
 */
@Data
public class LoginResponse {
    private boolean error;
    private String message;

    public LoginResponse(boolean error, String message) {
        this.error = error;
        this.message = message;
    }

    public LoginResponse(boolean error) {
        this.error = error;
    }
}
