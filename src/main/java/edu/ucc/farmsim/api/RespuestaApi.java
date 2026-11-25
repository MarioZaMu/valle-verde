package edu.ucc.farmsim.api;

import java.util.List;

/**
 * Cuerpo de toda respuesta exitosa: el valor de la operación y los eventos que causó, en orden (DT-19). El cliente
 * reconstruye el {@code Resultado} desde {@code datos} y republica {@code eventos} en su propio bus.
 *
 * <p>Stories: none.
 *
 * @param datos   el valor que devuelve la operación
 * @param eventos lo que pasó en el juego mientras corría la operación
 * @param <T>    tipo del valor
 */
public record RespuestaApi<T>(T datos, List<DtoEvento> eventos) { }
