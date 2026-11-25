package edu.ucc.farmsim.dominio.economia;

import edu.ucc.farmsim.dominio.tiempo.Estacion;

/**
 * Pattern: Strategy — Role: Strategy interface (how the price of one unit is worked out)
 *
 * <p>Los cultivos dependen de la estación, los productos animales no; el mercado elige la estrategia de cada producto
 * (RN-17).
 *
 * <p>Stories: HU-15.
 */
public interface EstrategiaPrecios {

    /**
     * El precio en m de una unidad.
     *
     * @param articulo   el producto
     * @param estacion la estación actual
     * @param demanda la demanda del producto, en porcentaje
     */
    int precioUnidad(TipoArticulo articulo, Estacion estacion, int demanda);

    /** {@code max(1, round_half_up(base × factorEstacion × demanda / 10 000))}, con cada factor en porcentaje. */
    static int precioDe(int base, int factorEstacion, int demanda) {
        long escalado = (long) base * factorEstacion * demanda;
        return (int) Math.max(1, (escalado + 5_000) / 10_000);
    }
}
