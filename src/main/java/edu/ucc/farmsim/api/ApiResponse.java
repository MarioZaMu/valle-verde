package edu.ucc.farmsim.api;

import java.util.List;

/**
 * Body of every successful response: the value of the operation and the events it caused, in order (DT-19).
 * The client rebuilds the {@code Result} from {@code data} and republishes {@code events} on its own bus.
 *
 * <p>Stories: none.
 *
 * @param data   the value returned by the operation
 * @param events what happened in the game while the operation ran
 * @param <T>    type of the value
 */
public record ApiResponse<T>(T data, List<EventDto> events) { }
