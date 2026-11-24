package edu.ucc.farmsim.application.dto;

/**
 * Un animal a la venta en el mercado.
 *
 * <p>Stories: HU-19, HU-38.
 *
 * @param animalId  id del animal, por ejemplo {@code HEN}
 * @param name      nombre en español
 * @param price     precio en m
 * @param freePlaces lugares que quedan en los edificios que pueden alojarlo
 */
public record AnimalOfferView(String animalId, String name, int price, int freePlaces) { }
