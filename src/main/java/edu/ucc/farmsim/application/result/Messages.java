package edu.ucc.farmsim.application.result;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Spanish message templates for every {@link ErrorCode} (RN-35). Placeholders use {@code %s} and {@code %d}.
 * A test compares them with the RN-35 table so the two never drift apart.
 *
 * <p>Stories: HU-08.
 */
public final class Messages {

    /** Shown when feeding a building where no animal needs food. */
    public static final String NOTHING_TO_FEED = "No hay animales por alimentar.";

    /** Shown when collecting from a building with nothing waiting. */
    public static final String NOTHING_TO_COLLECT = "No hay productos que recoger.";

    /** Shown when curing an animal that is not sick. */
    public static final String NOT_SICK = "Este animal no está enfermo.";

    /** The only line of the day summary when nothing happened. */
    public static final String NO_NEWS = "Sin novedades";

    private static final Locale SPANISH = Locale.forLanguageTag("es");
    private static final Map<ErrorCode, String> TEMPLATES = new EnumMap<>(ErrorCode.class);

    static {
        TEMPLATES.put(ErrorCode.SIN_PARTIDA, "No hay una partida en curso.");
        TEMPLATES.put(ErrorCode.FUERA_DE_ESTACION, "No se puede sembrar %s en %s: solo en %s.");
        TEMPLATES.put(ErrorCode.SIN_ENERGIA, "Energía insuficiente: necesitas %d y tienes %d.");
        TEMPLATES.put(ErrorCode.SIN_FONDOS, "Fondos insuficientes: faltan %d m.");
        TEMPLATES.put(ErrorCode.INVENTARIO_LLENO, "Inventario lleno: libera espacio o construye un Almacén.");
        TEMPLATES.put(ErrorCode.SIN_SEMILLAS, "No tienes semillas de %s.");
        TEMPLATES.put(ErrorCode.CANTIDAD_INVALIDA, "La cantidad debe estar entre 1 y %d.");
        TEMPLATES.put(ErrorCode.PARCELA_NO_CULTIVABLE, "Esta parcela no es de tierra de cultivo.");
        TEMPLATES.put(ErrorCode.PARCELA_OCUPADA, "La parcela ya está ocupada.");
        TEMPLATES.put(ErrorCode.YA_REGADO, "El cultivo ya está regado hoy.");
        TEMPLATES.put(ErrorCode.NO_MADURO, "El cultivo aún no está maduro: faltan %d días.");
        TEMPLATES.put(ErrorCode.SIN_CULTIVO, "No hay ningún cultivo en esta parcela.");
        TEMPLATES.put(ErrorCode.CULTIVO_MARCHITO, "El cultivo está marchito: límpialo para poder sembrar de nuevo.");
        TEMPLATES.put(ErrorCode.EDIFICIO_BLOQUEADO, "Edificio bloqueado: requiere el logro «%s».");
        TEMPLATES.put(ErrorCode.UBICACION_INVALIDA,
                "No se puede construir aquí: el área debe ser hierba libre dentro del mapa.");
        TEMPLATES.put(ErrorCode.LIMITE_EDIFICIOS, "Ya tienes el máximo de %s.");
        TEMPLATES.put(ErrorCode.SIN_ESPACIO_ANIMALES, "No hay espacio para más %s: construye o amplía el edificio.");
        TEMPLATES.put(ErrorCode.SIN_HENO, "Heno insuficiente: necesitas %d.");
        TEMPLATES.put(ErrorCode.SIN_MEDICINA, "No tienes Medicina.");
        TEMPLATES.put(ErrorCode.ANIMAL_ENFERMO, "Hay animales enfermos: cúralos con Medicina.");
        TEMPLATES.put(ErrorCode.MOVIMIENTO_BLOQUEADO, "No puedes pasar por ahí.");
        TEMPLATES.put(ErrorCode.ARCHIVO_INEXISTENTE, "No existe una partida en la ranura %d.");
        TEMPLATES.put(ErrorCode.ARCHIVO_CORRUPTO,
                "La partida está dañada y no se puede cargar. Tu partida actual no cambió.");
        TEMPLATES.put(ErrorCode.VERSION_NO_SOPORTADA, "La partida es de una versión no soportada.");
        TEMPLATES.put(ErrorCode.ERROR_ES, "No se pudo acceder a las partidas guardadas: %s.");
        TEMPLATES.put(ErrorCode.ERROR_INTERNO, "Ocurrió un error inesperado. Tu partida no cambió.");
    }

    private Messages() {
        // static helpers only
    }

    /** The raw template of a code, with its placeholders. */
    public static String template(ErrorCode code) {
        String template = TEMPLATES.get(code);
        if (template == null) {
            throw new IllegalStateException("No message for " + code);
        }
        return template;
    }

    /** The message of a code with its arguments filled in. */
    public static String of(ErrorCode code, Object... args) {
        return String.format(SPANISH, template(code), args);
    }
}
