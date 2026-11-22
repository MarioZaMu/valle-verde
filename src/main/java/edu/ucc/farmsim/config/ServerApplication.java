package edu.ucc.farmsim.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Spring Boot application of the server: it scans {@code edu.ucc.farmsim} for the controllers,
 * services and the repository, and starts the embedded web server (DT-16).
 *
 * <p>Stories: none.
 */
@SpringBootApplication(scanBasePackages = "edu.ucc.farmsim")
public class ServerApplication {
    // configuration lives in ServerConfig and application.properties
}
