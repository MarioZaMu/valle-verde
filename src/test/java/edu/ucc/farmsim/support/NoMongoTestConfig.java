package edu.ucc.farmsim.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * For Spring tests with the {@code no-mongo} profile: the saved games live in a {@link FakeSaveRepository},
 * so the API and the client are tested without a database.
 */
@TestConfiguration(proxyBeanMethods = false)
public class NoMongoTestConfig {

    /** The stand-in the tests can reach in to say which slots exist or that the storage is down. */
    @Bean
    public FakeSaveRepository saveRepository() {
        return new FakeSaveRepository();
    }
}
