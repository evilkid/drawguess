package com.wekay.drawguess.socket;

import com.wekay.drawguess.model.LoginResponse;
import com.wekay.drawguess.service.GameService;
import com.wekay.drawguess.service.WordService;
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
    private final WordService wordService;

    @GetMapping("/login")
    public LoginResponse login(@RequestParam(value = "username") String username) {
        return gameService.getUser(username)
                .map(user -> new LoginResponse(true, "Username already exists"))
                .orElse(new LoginResponse(false));
    }
}
