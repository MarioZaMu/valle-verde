package edu.ucc.farmsim.config;

import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.domain.game.DayPipeline;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the beans that are not components of their own: the server's event bus and the end-of-day
 * pipeline (DT-16). The domain stays free of Spring, so its objects are created here.
 *
 * <p>Stories: none.
 */
@Configuration(proxyBeanMethods = false)
public class ServerConfig {

    /** The bus the services publish to; the {@code EventCollector} listens to it. */
    @Bean
    public EventBus eventBus() {
        return new EventBus();
    }

    /** The clock the saves are stamped with. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    /** The ordered steps that run when a day ends (RN-03). */
    @Bean
    public DayPipeline dayPipeline() {
        return DayPipeline.standard();
    }
}
