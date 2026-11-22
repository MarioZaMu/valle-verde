package edu.ucc.farmsim.application.result;

/**
 * Every reason an operation can be rejected (RN-35). A rejection never changes the game state.
 *
 * <p>Stories: HU-08.
 */
public enum ErrorCode {
    SIN_PARTIDA,
    FUERA_DE_ESTACION,
    SIN_ENERGIA,
    SIN_FONDOS,
    INVENTARIO_LLENO,
    SIN_SEMILLAS,
    CANTIDAD_INVALIDA,
    PARCELA_NO_CULTIVABLE,
    PARCELA_OCUPADA,
    YA_REGADO,
    NO_MADURO,
    SIN_CULTIVO,
    CULTIVO_MARCHITO,
    EDIFICIO_BLOQUEADO,
    UBICACION_INVALIDA,
    LIMITE_EDIFICIOS,
    SIN_ESPACIO_ANIMALES,
    SIN_HENO,
    SIN_MEDICINA,
    ANIMAL_ENFERMO,
    ARCHIVO_INEXISTENTE,
    ARCHIVO_CORRUPTO,
    VERSION_NO_SOPORTADA,
    ERROR_ES,
    ERROR_INTERNO
}
