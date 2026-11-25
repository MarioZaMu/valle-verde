package edu.ucc.farmsim.dominio.economia;

import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import edu.ucc.farmsim.dominio.tiempo.Estacion;

/**
 * Pattern: Strategy — Role: ConcreteStrategy (crops: 90 % in season, 120 % out of season)
 *
 * <p>Stories: HU-15.
 */
public final class PreciosCultivo implements EstrategiaPrecios {

    @Override
    public int precioUnidad(TipoArticulo articulo, Estacion estacion, int demanda) {
        int factor = articulo.tipoCultivo().estacionesValidos().contains(estacion)
                ? ReglasJuego.FACTOR_ESTACION_EN_ESTACION
                : ReglasJuego.FUERA_FACTOR_ESTACION_DE_ESTACION;
        return EstrategiaPrecios.precioDe(articulo.tipoCultivo().precioBaseVender(), factor, demanda);
    }
}
