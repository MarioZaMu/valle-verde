package edu.ucc.farmsim.configuracion;

import edu.ucc.farmsim.aplicacion.evento.BusEvento;
import edu.ucc.farmsim.dominio.juego.SecuenciaDia;
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
public class ConfiguracionServidor {

    /** El bus donde publican los servicios; el {@code RecolectorEvento} lo escucha. */
    @Bean
    public BusEvento busEvento() {
        return new BusEvento();
    }

    /** El reloj con el que se sellan los guardados. */
    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }

    /** Los pasos ordenados que corren cuando termina un día (RN-03). */
    @Bean
    public SecuenciaDia secuenciaDia() {
        return SecuenciaDia.estandar();
    }
}
