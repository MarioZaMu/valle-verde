package edu.ucc.farmsim.aplicacion.resultado;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Plantillas de mensajes en español para cada {@link CodigoError} (RN-35). Los marcadores usan {@code %s} y {@code %d}.
 * Una prueba las compara con la tabla de RN-35 para que nunca se desfasen.
 *
 * <p>Stories: HU-08.
 */
public final class Mensajes {

    /** Se muestra al alimentar un edificio donde ningún animal necesita comida. */
    public static final String NADA_A_ALIMENTAR = "No hay animales por alimentar.";

    /** Se muestra al recoger de un edificio sin nada esperando. */
    public static final String NADA_A_RECOLECTAR = "No hay productos que recoger.";

    /** Se muestra al curar a un animal que no está enfermo. */
    public static final String NO_ENFERMO = "Este animal no está enfermo.";

    /** La única línea del resumen del día cuando no pasó nada. */
    public static final String NO_NOTICIAS = "Sin novedades";

    private static final Locale ESPANIOL = Locale.forLanguageTag("es");
    private static final Map<CodigoError, String> PLANTILLAS = new EnumMap<>(CodigoError.class);

    static {
        PLANTILLAS.put(CodigoError.SIN_PARTIDA, "No hay una partida en curso.");
        PLANTILLAS.put(CodigoError.FUERA_DE_ESTACION, "No se puede sembrar %s en %s: solo en %s.");
        PLANTILLAS.put(CodigoError.SIN_ENERGIA, "Energía insuficiente: necesitas %d y tienes %d.");
        PLANTILLAS.put(CodigoError.SIN_FONDOS, "Fondos insuficientes: faltan %d m.");
        PLANTILLAS.put(CodigoError.INVENTARIO_LLENO, "Inventario lleno: libera espacio o construye un Almacén.");
        PLANTILLAS.put(CodigoError.SIN_SEMILLAS, "No tienes semillas de %s.");
        PLANTILLAS.put(CodigoError.CANTIDAD_INVALIDA, "La cantidad debe estar entre 1 y %d.");
        PLANTILLAS.put(CodigoError.PARCELA_NO_CULTIVABLE, "Esta parcela no es de tierra de cultivo.");
        PLANTILLAS.put(CodigoError.PARCELA_OCUPADA, "La parcela ya está ocupada.");
        PLANTILLAS.put(CodigoError.YA_REGADO, "El cultivo ya está regado hoy.");
        PLANTILLAS.put(CodigoError.NO_MADURO, "El cultivo aún no está maduro: faltan %d días.");
        PLANTILLAS.put(CodigoError.SIN_CULTIVO, "No hay ningún cultivo en esta parcela.");
        PLANTILLAS.put(CodigoError.CULTIVO_MARCHITO, "El cultivo está marchito: límpialo para poder sembrar de nuevo.");
        PLANTILLAS.put(CodigoError.EDIFICIO_BLOQUEADO, "Edificio bloqueado: requiere el logro «%s».");
        PLANTILLAS.put(CodigoError.UBICACION_INVALIDA,
                "No se puede construir aquí: el área debe ser hierba libre dentro del mapa.");
        PLANTILLAS.put(CodigoError.LIMITE_EDIFICIOS, "Ya tienes el máximo de %s.");
        PLANTILLAS.put(CodigoError.SIN_ESPACIO_ANIMALES, "No hay espacio para más %s: construye o amplía el edificio.");
        PLANTILLAS.put(CodigoError.SIN_HENO, "Heno insuficiente: necesitas %d.");
        PLANTILLAS.put(CodigoError.SIN_MEDICINA, "No tienes Medicina.");
        PLANTILLAS.put(CodigoError.ANIMAL_ENFERMO, "Hay animales enfermos: cúralos con Medicina.");
        PLANTILLAS.put(CodigoError.MOVIMIENTO_BLOQUEADO, "No puedes pasar por ahí.");
        PLANTILLAS.put(CodigoError.NPC_LEJOS, "Acércate a %s para hablar.");
        PLANTILLAS.put(CodigoError.ENCARGO_INCOMPLETO, "Te faltan %s para el encargo.");
        PLANTILLAS.put(CodigoError.ARCHIVO_INEXISTENTE, "No existe una partida en la ranura %d.");
        PLANTILLAS.put(CodigoError.ARCHIVO_CORRUPTO,
                "La partida está dañada y no se puede cargar. Tu partida actual no cambió.");
        PLANTILLAS.put(CodigoError.VERSION_NO_SOPORTADA, "La partida es de una versión no soportada.");
        PLANTILLAS.put(CodigoError.ERROR_ES, "No se pudo acceder a las partidas guardadas: %s.");
        PLANTILLAS.put(CodigoError.ERROR_INTERNO, "Ocurrió un error inesperado. Tu partida no cambió.");
    }

    private Mensajes() {
                // solo métodos de utilidad estáticos
    }

    /** La plantilla en bruto de un código, con sus marcadores. */
    public static String plantilla(CodigoError codigo) {
        String plantilla = PLANTILLAS.get(codigo);
        if (plantilla == null) {
            throw new IllegalStateException("No message for " + codigo);
        }
        return plantilla;
    }

    /** El mensaje de un código con sus argumentos ya rellenados. */
    public static String of(CodigoError codigo, Object... args) {
        return String.format(ESPANIOL, plantilla(codigo), args);
    }
}
