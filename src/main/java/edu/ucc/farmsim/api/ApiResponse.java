package edu.ucc.farmsim.api;

import java.util.List;

/**
 * Cuerpo de toda respuesta exitosa: el valor de la operación y los eventos que causó, en orden (DT-19). El cliente
 * reconstruye el {@code Result} desde {@code data} y republica {@code events} en su propio bus.
 *
 * <p>Stories: none.
 *
 * @param data   el valor que devuelve la operación
 * @param events lo que pasó en el juego mientras corría la operación
 * @param <T>    tipo del valor
 */
public record ApiResponse<T>(T data, List<EventDto> events) { }
