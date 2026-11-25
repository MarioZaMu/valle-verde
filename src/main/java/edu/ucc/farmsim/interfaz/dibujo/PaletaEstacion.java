package edu.ucc.farmsim.interfaz.dibujo;

import edu.ucc.farmsim.interfaz.tema.Paleta;
import java.awt.Color;

/**
 * El aspecto de cada estación en el mapa (HU-40 · CA-1): el pasto cambia de color y el clima agrega un tinte. La
 * primavera conserva la paleta base.
 *
 * <p>Stories: HU-40.
 */
final class PaletaEstacion {

    private static final Color HIERBA_VERANO = new Color(0x86B33C);
    private static final Color HIERBA_OTONIO = new Color(0xB98A3E);
    private static final Color HIERBA_INVIERNO = new Color(0xD8E6EE);

    private PaletaEstacion() {
                // solo métodos de utilidad estáticos
    }

    /** Color del pasto de un id de estación; los ids desconocidos usan el color de primavera. */
    static Color hierba(String estacion) {
        if (estacion == null) {
            return Paleta.GRASS;
        }
        return switch (estacion) {
            case "SUMMER" -> HIERBA_VERANO;
            case "AUTUMN" -> HIERBA_OTONIO;
            case "WINTER" -> HIERBA_INVIERNO;
            default -> Paleta.GRASS;
        };
    }

    /** Tinte translúcido que se coloca sobre todo el mapa para un id de clima, o {@code null} si no tiñe. */
    static Color tinte(String clima) {
        if (clima == null) {
            return null;
        }
        return switch (clima) {
            case "RAINY" -> new Color(30, 60, 120, 45);
            case "CLOUDY" -> new Color(90, 90, 100, 35);
            case "DROUGHT" -> new Color(255, 170, 40, 50);
            case "FROST" -> new Color(150, 200, 255, 60);
            case "SNOWY" -> new Color(255, 255, 255, 30);
            default -> null;
        };
    }
}
