package edu.ucc.farmsim.aplicacion.dto;

import java.util.List;

/**
 * Los edificios de la granja y el catálogo de lo que se puede construir.
 *
 * <p>Stories: HU-18.
 *
 * @param edificios todos los edificios construidos, en orden
 * @param catalogo   una oferta por tipo de edificio
 */
public record VistaEdificios(List<VistaEdificio> edificios, List<VistaOfertaEdificio> catalogo) { }
