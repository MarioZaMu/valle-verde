package edu.ucc.farmsim.aplicacion.resultado;

/**
 * Toda razón por la que se puede rechazar una operación (RN-35). Un rechazo nunca cambia el estado de la partida, con
 * una excepción que pide RN-36: un paso bloqueado ({@link #MOVIMIENTO_BLOQUEADO}) igual hace girar al avatar.
 *
 * <p>Stories: HU-08, HU-45, HU-46, HU-47.
 */
public enum CodigoError {
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
    MOVIMIENTO_BLOQUEADO,
    NPC_LEJOS,
    ENCARGO_INCOMPLETO,
    ARCHIVO_INEXISTENTE,
    ARCHIVO_CORRUPTO,
    VERSION_NO_SOPORTADA,
    ERROR_ES,
    ERROR_INTERNO
}
