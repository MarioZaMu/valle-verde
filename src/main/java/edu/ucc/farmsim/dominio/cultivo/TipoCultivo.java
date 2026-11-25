package edu.ucc.farmsim.dominio.cultivo;

import edu.ucc.farmsim.dominio.tiempo.Estacion;
import java.util.EnumSet;
import java.util.Set;

/**
 * Catálogo de cultivos (RN-05): días para madurar y las estaciones en que se puede sembrar cada uno. Este enum es una
 * tabla de configuración; los precios de semillas y cosechas los agregan las historias de economía.
 *
 * <p>Stories: HU-01.
 */
public enum TipoCultivo {
    CARROT(4, 10, 30, Estacion.SPRING),
    POTATO(6, 20, 55, Estacion.SPRING),
    TOMATO(7, 25, 65, Estacion.SUMMER),
    CORN(8, 30, 75, Estacion.SUMMER, Estacion.AUTUMN),
    PUMPKIN(10, 40, 110, Estacion.AUTUMN);

    private final int diasAMaduro;
    private final int precioSemilla;
    private final int precioBaseVender;
    private final Set<Estacion> estacionesValidos;

    TipoCultivo(int diasAMaduro, int precioSemilla, int precioBaseVender, Estacion first, Estacion... mas) {
        this.diasAMaduro = diasAMaduro;
        this.precioSemilla = precioSemilla;
        this.precioBaseVender = precioBaseVender;
        this.estacionesValidos = EnumSet.of(first, mas);
    }

    /** Días que hay que regar un cultivo para que madure (T en RN-08). */
    public int diasAMaduro() {
        return diasAMaduro;
    }

    /** Estaciones en que se puede sembrar este cultivo y sigue vivo. */
    /** Precio en m de una semilla en el mercado (RN-05, RN-16). */
    public int precioSemilla() {
        return precioSemilla;
    }

    /** Precio base de venta en m de una unidad cosechada (RN-05, RN-17). */
    public int precioBaseVender() {
        return precioBaseVender;
    }

    public Set<Estacion> estacionesValidos() {
        return EnumSet.copyOf(estacionesValidos);
    }

    /** Día de crecimiento en que el cultivo deja la etapa Brote: {@code ceil(T / 2)} (RN-08). */
    public int diasAMedias() {
        return (diasAMaduro + 1) / 2;
    }
}
