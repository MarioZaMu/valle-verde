package edu.ucc.farmsim.config;

import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.domain.game.DayPipeline;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara los beans que no son componentes por sí mismos: el bus de eventos del servidor y el pipeline de fin de día
 * (DT-16). El dominio queda libre de Spring, por eso sus objetos se crean aquí.
 *
 * <p>Stories: none.
 */
@Configuration(proxyBeanMethods = false)
public class ServerConfig {

    /** El bus donde publican los servicios; el {@code EventCollector} lo escucha. */
    @Bean
    public EventBus eventBus() {
        return new EventBus();
    }

    /** El reloj con el que se sellan los guardados. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    /** Los pasos ordenados que corren cuando termina un día (RN-03). */
    @Bean
    public DayPipeline dayPipeline() {
        return DayPipeline.standard();
    }
}
