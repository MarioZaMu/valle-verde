package edu.ucc.farmsim.dominio.edificio;

import edu.ucc.farmsim.dominio.animal.TipoAnimal;
import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import edu.ucc.farmsim.dominio.progreso.Logro;

/**
 * Catálogo de edificios (RN-20): tamaño, costo, el máximo que se puede construir, el logro que lo desbloquea (RN-29) y
 * el animal que aloja, si lo hay.
 *
 * <p>Stories: HU-18.
 */
public enum TipoEdificio {
    COOP(2, 2, ReglasJuego.COSTO_GALLINERO, ReglasJuego.GALLINERO_MAXIMO, null, TipoAnimal.HEN),
    BARN(3, 2, ReglasJuego.COSTO_ESTABLO, ReglasJuego.ESTABLO_MAXIMO, Logro.GREEN_THUMB, TipoAnimal.COW),
    WAREHOUSE(2, 2, ReglasJuego.COSTO_ALMACEN, ReglasJuego.ALMACEN_MAXIMO, Logro.FIRST_HARVEST, null),
    GREENHOUSE(3, 3, ReglasJuego.COSTO_INVERNADERO, ReglasJuego.INVERNADERO_MAXIMO, Logro.CAPITALIST, null);

    private final int width;
    private final int height;
    private final int costo;
    private final int conteoMaximo;
    private final Logro requisito;
    private final TipoAnimal animalAlojado;

    TipoEdificio(int width, int height, int costo, int conteoMaximo, Logro requisito, TipoAnimal animalAlojado) {
        this.width = width;
        this.height = height;
        this.costo = costo;
        this.conteoMaximo = conteoMaximo;
        this.requisito = requisito;
        this.animalAlojado = animalAlojado;
    }

    /** Columnas que cubre el edificio. */
    public int width() {
        return width;
    }

    /** Filas que cubre el edificio. */
    public int height() {
        return height;
    }

    /** Costo en m. */
    public int costo() {
        return costo;
    }

    /** El máximo de edificios de este tipo que puede tener el jugador. */
    public int conteoMaximo() {
        return conteoMaximo;
    }

    /** El logro que lo desbloquea, o {@code null} si está disponible desde el inicio. */
    public Logro requisito() {
        return requisito;
    }

    /** El animal que aloja este edificio, o {@code null} si no aloja ninguno. */
    public TipoAnimal animalAlojado() {
        return animalAlojado;
    }

    /** El edificio donde vive un animal. */
    public static TipoEdificio alojamiento(TipoAnimal animal) {
        for (TipoEdificio tipo : values()) {
            if (tipo.animalAlojado == animal) {
                return tipo;
            }
        }
        throw new IllegalStateException("No building houses " + animal);
    }
}
