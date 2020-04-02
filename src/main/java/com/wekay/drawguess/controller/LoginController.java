package com.wekay.drawguess.controller;

import com.wekay.drawguess.model.LoginResponse;
import com.wekay.drawguess.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ouerghi Yassine
 */
@RestController
@RequiredArgsConstructor
public class LoginController {

    private final GameService gameService;

    @GetMapping("/login")
    public LoginResponse login(@RequestParam(value = "username") String username) {
        return gameService.getUser(username.trim())
                .map(user -> new LoginResponse(true, "Username already exists"))
                .orElse(new LoginResponse(false));
    }
}
