package edu.ucc.farmsim.dominio.economia;

import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import edu.ucc.farmsim.dominio.tiempo.Estacion;

/**
 * Pattern: Strategy — Role: ConcreteStrategy (animal products: the season does not matter)
 *
 * <p>Stories: HU-15.
 */
public final class PreciosFijos implements EstrategiaPrecios {

    @Override
    public int precioUnidad(TipoArticulo articulo, Estacion estacion, int demanda) {
        int base = switch (articulo) {
            case EGG -> ReglasJuego.HUEVO_PRECIO_BASE;
            case MILK -> ReglasJuego.LECHE_PRECIO_BASE;
            default -> throw new IllegalArgumentException("Not an animal product: " + articulo);
        };
        return EstrategiaPrecios.precioDe(base, ReglasJuego.FACTOR_ESTACION_PLANO, demanda);
    }
}
