package edu.ucc.farmsim.aplicacion.servicio;

import edu.ucc.farmsim.dominio.juego.EstadoJuego;
import org.springframework.stereotype.Component;

/**
 * Guarda la partida que se está jugando. Los servicios leen el estado desde aquí; cargar una partida construye un
 * estado nuevo aparte y lo cambia con {@link #replace(EstadoJuego)} solo cuando está completo y es válido.
 *
 * <p>Stories: HU-28.
 */
@Component
public class SesionJuego {

    private EstadoJuego actual;

    /** Verdadero si hay una partida en curso. */
    public boolean tieneJuego() {
        return actual != null;
    }

    /**
     * La partida en curso.
     *
     * @throws IllegalStateException si no hay ninguna (quien llama comprueba antes {@link #tieneJuego()})
     */
    public EstadoJuego actual() {
        if (actual == null) {
            throw new IllegalStateException("No game in progress");
        }
        return actual;
    }

    /** Convierte el estado dado en la partida en curso. */
    public void replace(EstadoJuego estado) {
        this.actual = estado;
    }
}
