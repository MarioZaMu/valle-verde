package edu.ucc.farmsim.apoyo;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Para las pruebas de Spring con el perfil {@code no-mongo}: las partidas guardadas viven en un {@link
 * RepositorioGuardadoFalso}, así que la API y el cliente se prueban sin base de datos.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ConfiguracionTestSinMongo {

    /**
     * El sustituto al que las pruebas pueden llegar para decir qué ranuras existen o que el almacenamiento está caído.
     */
    @Bean
    public RepositorioGuardadoFalso guardarRepositorio() {
        return new RepositorioGuardadoFalso();
    }
}
