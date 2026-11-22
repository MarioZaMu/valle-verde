package edu.ucc.farmsim.application.dto;

/**
 * One animal for sale in the market.
 *
 * <p>Stories: HU-19, HU-38.
 *
 * @param animalId  animal id, for example {@code HEN}
 * @param name      Spanish name
 * @param price     price in m
 * @param freePlaces places left in the buildings that can house it
 */
public record AnimalOfferView(String animalId, String name, int price, int freePlaces) { }
