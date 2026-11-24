package edu.ucc.farmsim.application.dto;

import java.util.List;

/**
 * Los edificios de la granja y el catálogo de lo que se puede construir.
 *
 * <p>Stories: HU-18.
 *
 * @param buildings todos los edificios construidos, en orden
 * @param catalog   una oferta por tipo de edificio
 */
public record BuildingsView(List<BuildingView> buildings, List<BuildingOfferView> catalog) { }
