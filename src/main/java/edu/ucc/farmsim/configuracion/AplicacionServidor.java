package edu.ucc.farmsim.configuracion;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * La aplicación Spring Boot del servidor: escanea {@code edu.ucc.farmsim} en busca de los controladores, servicios y el
 * repositorio, y arranca el servidor web embebido (DT-16).
 *
 * <p>Stories: none.
 */
@SpringBootApplication(scanBasePackages = "edu.ucc.farmsim")
public class AplicacionServidor {
        // la configuración vive en ServerConfig y application.properties
}
