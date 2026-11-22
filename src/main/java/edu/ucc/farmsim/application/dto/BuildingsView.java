package edu.ucc.farmsim.application.dto;

import java.util.List;

/**
 * The buildings of the farm and the catalogue of what can be built.
 *
 * <p>Stories: HU-18.
 *
 * @param buildings every building built, in order
 * @param catalog   one offer per type of building
 */
public record BuildingsView(List<BuildingView> buildings, List<BuildingOfferView> catalog) { }
