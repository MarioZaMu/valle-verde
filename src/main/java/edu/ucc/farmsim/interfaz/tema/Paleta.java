package edu.ucc.farmsim.interfaz.tema;

import java.awt.Color;

/**
 * Los colores del juego dibujados con Java2D. Cada terreno y etapa de cultivo tiene su propio color o forma, de modo
 * que el mapa nunca depende solo del color (RNF-14).
 *
 * <p>Stories: HU-35, HU-39, HU-43.
 */
public final class Paleta {

    /** Detrás del mapa. */
    public static final Color FONDO = new Color(0x2B3A2B);
    /** Pasto. */
    public static final Color GRASS = new Color(0x6AAE4B);
    /** Tierra, seca. */
    public static final Color SOIL = new Color(0x8B5A2B);
    /** Tierra oscurecida por el riego. */
    public static final Color TIERRA_REGADA = new Color(0x5C3A1A);
    /** Líneas de surco sobre la tierra. */
    public static final Color SURCO = new Color(0x70471F);
    /** Agua. */
    public static final Color WATER = new Color(0x3A7BD5);
    /** Líneas de ola sobre el agua. */
    public static final Color ONDA = new Color(0xA9CBF2);
    /** Suelo de obstáculos (rocas y árboles). */
    public static final Color OBSTACLE = new Color(0x2F5D2F);
    /** Tronco de árbol. */
    public static final Color TRONCO = new Color(0x5A3A1E);
    /** Copa de árbol y hojas de las plantas. */
    public static final Color HOJA = new Color(0x3FA34D);
    /** Una semilla. */
    public static final Color SEED = new Color(0x3B2410);
    /** Una planta marchita. */
    public static final Color WITHERED = new Color(0xA68B5B);
    /** Línea de la grilla entre parcelas. */
    public static final Color GRILLA = new Color(0, 0, 0, 40);
    /** Borde de la parcela bajo el cursor. */
    public static final Color SOBREVOLAR = new Color(0xFFFFFF);

    /** Pared de un gallinero. */
    public static final Color PARED_GALLINERO = new Color(0xD9A066);

    /** Pared de un establo. */
    public static final Color PARED_ESTABLO = new Color(0xB5473B);

    /** Pared de un almacén. */
    public static final Color PARED_ALMACEN = new Color(0x9A8F7A);

    /** Marco y vidrio de un invernadero. */
    public static final Color VIDRIO_INVERNADERO = new Color(0xBDE8E0);

    /** Techo de todos los edificios. */
    public static final Color TECHO = new Color(0x5B2E1F);

    /** Contorno de un edificio. */
    public static final Color CONTORNO_EDIFICIO = new Color(0x2A1A10);

    /** Un animal sano. */
    public static final Color ANIMAL_SANO = new Color(0xFFFDF2);

    /** Un animal hambriento. */
    public static final Color ANIMAL_HAMBRIENTO = new Color(0xF2C14E);

    /** Un animal enfermo. */
    public static final Color ANIMAL_ENFERMO = new Color(0x7FA36B);

    /** Marcador de productos que esperan ser recogidos. */
    public static final Color PENDIENTE = new Color(0xFFD23F);

    /** Texto de un aviso (HU-39, HU-43). */
    public static final Color TEXTO_AVISO = new Color(0xFFFFFF);

    /** Fondo de un aviso de error. */
    public static final Color ERROR_AVISO = new Color(0xA33A32);

    /** Fondo de un aviso de éxito. */
    public static final Color EXITO_AVISO = new Color(0x2E7D4F);

    /** Fondo de un aviso de logro. */
    public static final Color LOGRO_AVISO = new Color(0x8A6A12);

    /** Fondo de un aviso informativo. */
    public static final Color INFO_AVISO = new Color(0x2F4F6F);

    /** Texto de la barra superior. */
    public static final Color TEXTO_HUD = new Color(0xFFF4D6);

    /** Fondo de la barra superior. */
    public static final Color FONDO_HUD = new Color(0x3B2F1E);

    /** Texto de los diálogos. */
    public static final Color TEXTO_DIALOGO = new Color(0x2B2118);

    /** Fondo de los diálogos. */
    public static final Color FONDO_DIALOGO = new Color(0xFFF8E7);

    private Paleta() {
                // contenedor de constantes
    }

    /** Color del fruto de cada cultivo, por id de cultivo; los cultivos desconocidos usan un color neutro. */
    public static Color cultivo(String idCultivo) {
        return switch (idCultivo) {
            case "CARROT" -> new Color(0xF28C28);
            case "POTATO" -> new Color(0xC9A66B);
            case "TOMATO" -> new Color(0xD0312D);
            case "CORN" -> new Color(0xF2D43D);
            case "PUMPKIN" -> new Color(0xE8751A);
            default -> Color.GRAY;
        };
    }
}
