package edu.ucc.farmsim.aplicacion.dto;

import java.util.List;

/**
 * Todo lo que muestra el diálogo del mercado: lo que se puede vender y lo que se puede comprar.
 *
 * <p>Stories: HU-14, HU-15, HU-16, HU-19, HU-38.
 *
 * @param vende   los productos vendibles con su precio y tendencia
 * @param compra    el catálogo de compra
 * @param animales los animales en venta
 */
public record VistaMercado(List<VistaOfertaVender> vende, List<VistaOfertaComprar> compra, List<VistaOfertaAnimal> animales) {

    /** Un mercado sin animales. */
    public VistaMercado(List<VistaOfertaVender> vende, List<VistaOfertaComprar> compra) {
        this(vende, compra, List.of());
    }
}
