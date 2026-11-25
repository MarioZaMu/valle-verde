package edu.ucc.farmsim.aplicacion.dto;

/**
 * Un animal a la venta en el mercado.
 *
 * <p>Stories: HU-19, HU-38.
 *
 * @param animalId  id del animal, por ejemplo {@code HEN}
 * @param name      nombre en español
 * @param precio     precio en m
 * @param lugaresLibres lugares que quedan en los edificios que pueden alojarlo
 */
public record VistaOfertaAnimal(String animalId, String name, int precio, int lugaresLibres) { }
