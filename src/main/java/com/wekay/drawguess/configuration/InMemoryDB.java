package com.wekay.drawguess.configuration;

import com.wekay.drawguess.session.GameSession;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * @author Ouerghi Yassine
 */
@Configuration
public class InMemoryDB {

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public GameSession gameSession() {
        return new GameSession();
    }
}
