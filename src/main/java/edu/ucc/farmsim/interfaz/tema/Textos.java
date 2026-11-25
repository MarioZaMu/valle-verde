package edu.ucc.farmsim.interfaz.tema;

import edu.ucc.farmsim.aplicacion.resultado.Nombres;
import edu.ucc.farmsim.interfaz.componentes.Herramienta;
import java.util.Locale;

/**
 * Todo texto que el jugador lee en pantalla, en español. Tenerlos aquí significa que las pantallas no contienen
 * literales y que el idioma es fácil de revisar (RNF-10).
 *
 * <p>Stories: HU-33, HU-34, HU-36, HU-45, HU-46, HU-47, HU-49.
 */
public final class Textos {

    /** Pantalla de inicio: empezar una partida nueva. */
    public static final String JUEGO_INICIO_NUEVO = "Nueva partida";

    /** Pantalla de inicio: empezar con una granja de demostración ya construida. */
    public static final String DEMOSTRACION_INICIO = "Granja de demostración";

    /** Pantalla de inicio: cargar una partida guardada. */
    public static final String CARGAR_INICIO = "Cargar";

    /** Pantalla de inicio: salir de la aplicación. */
    public static final String SALIR_INICIO = "Salir";

    /** Línea bajo el nombre del juego en la pantalla de inicio. */
    public static final String SUBTITULO_INICIO = "Siembra, cuida tus animales y haz crecer tu granja";

    /** Se muestra cuando se elige «Cargar» y ninguna ranura contiene una partida. */
    public static final String NO_JUEGOS_ARCHIVADOS = "No hay partidas guardadas.";

    /** Se pregunta antes de descartar una partida en curso. */
    public static final String JUEGO_DESCARTAR_CONFIRMAR =
            "Ya hay una partida en curso. ¿Descartarla y empezar una nueva?";

    /** Pantalla de juego: volver a la pantalla de inicio. */
    public static final String MENU = "Menú";

    /** Pantalla de juego: terminar el día. */
    public static final String END_DAY = "Terminar día";

    /** Pantalla de juego: texto temporal donde se dibujará el mapa de la granja. */
    public static final String MARCADOR_MAPA = "Aquí aparecerá el mapa de la granja.";

    /** Se muestra cuando se usa la herramienta Sembrar y no hay semilla elegida. */
    public static final String NO_SEMILLA_SELECCIONADA = "Elige una semilla en la barra o en el inventario (I).";

    /** Título del diálogo del inventario. */
    public static final String TITULO_INVENTARIO = "Inventario";

    /** Título del diálogo del mercado. */
    public static final String TITULO_MERCADO = "Mercado";

    /** Pestaña para comprar. */
    public static final String COMPRAR_PESTANIA = "Comprar";

    /** Pestaña para vender. */
    public static final String VENDER_PESTANIA = "Vender";

    /** Etiqueta del selector de cantidad. */
    public static final String CANTIDAD = "Cantidad:";

    /** Botón que compra. */
    public static final String COMPRAR = "Comprar";

    /** Botón que vende. */
    public static final String VENDER = "Vender";

    /** Columna de la tabla: producto. */
    public static final String PRODUCTO_COL = "Producto";

    /** Columna de la tabla: precio. */
    public static final String PRECIO_COL = "Precio";

    /** Columna de la tabla: unidades que se tienen. */
    public static final String COL_PROPIO = "En inventario";

    /** Columna de la tabla: tendencia. */
    public static final String TENDENCIA_COL = "Tendencia";

    /** Etiqueta del selector de semilla junto a las herramientas. */
    public static final String ETIQUETA_SEMILLA = "Semilla:";

    /** Se muestra al alimentar o recoger en una parcela sin edificio. */
    public static final String NO_AQUI_EDIFICIO = "No hay ningún edificio en esta parcela.";

    /** Se muestra cuando se pulsa E y nadie está junto al avatar. */
    public static final String NADIE_A_HABLAR = "No hay nadie cerca para hablar: acércate a alguien.";

    /** Botón que entrega un encargo. */
    public static final String ENTREGAR = "Entregar";

    /** Se muestra en el diálogo después de entregar un encargo. */
    public static final String ENCARGO_ENTREGADO = "¡Encargo entregado!";

    /** Aviso de un encargo entregado. */
    public static String encargoCompletado(String name, int recompensa) {
        return "Encargo de " + name + " cumplido: +" + dinero(recompensa);
    }

    /** Botón del cuadro del avatar que termina el tutorial de inicio. */
    public static final String TUTORIAL_SALTAR = "Saltar tutorial";

    /** Dónde va el jugador en el tutorial de inicio, por ejemplo «Paso 2 de 5». */
    public static String pasoTutorial(int numero, int total) {
        return "Paso " + numero + " de " + total;
    }

    /** Se muestra cuando se pulsa Espacio y el avatar mira fuera de la granja. */
    public static final String NADA_EN_FRENTE = "No hay ninguna parcela frente a ti.";

    /** Título del diálogo de construcción. */
    public static final String TITULO_CONSTRUCCION = "Construir";

    /** Columna de la tabla: edificio. */
    public static final String EDIFICIO_COL = "Edificio";

    /** Columna de la tabla: tamaño. */
    public static final String TAMANIO_COL = "Tamaño";

    /** Columna de la tabla: costo. */
    public static final String COSTO_COL = "Costo";

    /** Columna de la tabla: estado de una oferta de edificio. */
    public static final String ESTADO_COL = "Estado";

    /** Botón que construye. */
    public static final String BUILD = "Construir aquí";

    /** Título del panel del edificio. */
    public static final String TITULO_EDIFICIO = "Edificio";

    /** Columna de la tabla: animal. */
    public static final String COL_ANIMAL = "Animal";

    /** Columna de la tabla: días sin comida. */
    public static final String DIAS_COL_SIN_ALIMENTAR = "Días sin comer";

    /** Columna de la tabla: comió hoy. */
    public static final String HOY_COL_ALIMENTADO = "Comió hoy";

    /** Columna de la tabla: unidades esperando. */
    public static final String COL_PENDIENTE = "Pendiente";

    /** Botón que alimenta un edificio. */
    public static final String EDIFICIO_ALIMENTAR = "Alimentar";

    /** Botón que recoge los productos de un edificio. */
    public static final String RECOLECTAR = "Recolectar";

    /** Botón que cura al animal seleccionado. */
    public static final String CURAR = "Curar";

    /** Título del resumen del día. */
    public static final String TITULO_RESUMEN = "Resumen del día";

    /** Botón que cierra un diálogo. */
    public static final String CERRAR = "Cerrar";

    /** Sí. */
    public static final String SI = "sí";

    /** No. */
    public static final String NO = "no";

    /** Título del diálogo de logros. */
    public static final String TITULO_LOGROS = "Logros";

    /** Columna de la tabla: logro. */
    public static final String LOGRO_COL = "Logro";

    /** Columna de la tabla: lo que pide. */
    public static final String CONDICION_COL = "Condición";

    /** Columna de la tabla: recompensa. */
    public static final String RECOMPENSA_COL = "Recompensa";

    /** Estado de un logro desbloqueado. */
    public static final String DESBLOQUEADO = "Desbloqueado";

    /** Título del diálogo de ranuras de guardado. */
    public static final String TITULO_RANURAS = "Partidas guardadas";

    /** Botón que guarda. */
    public static final String GUARDADO = "Guardar";

    /** Tooltip del botón Guardar de la pantalla de juego, con sus teclas rápidas. */
    public static final String PISTA_GUARDADO = "Guardar la partida en una ranura (F5 guarda rápido, F9 carga)";

    /** Botón que carga. */
    public static final String CARGAR = "Cargar";

    /** Una ranura sin nada. */
    public static final String RANURA_VACIA = "Vacía";

    /** Una ranura cuyo documento no se puede leer. */
    public static final String RANURA_DANADA = "Dañada";

    /** Se pregunta antes de reemplazar una ranura ocupada. */
    public static final String SOBRESCRIBIR_CONFIRMAR = "La ranura ya tiene una partida. ¿Reemplazarla?";

    /** Inicio del mensaje después de guardar; sigue el número de ranura. */
    public static final String ARCHIVADO_EN_RANURA = "Partida guardada en la ranura ";

    /** Inicio del mensaje después de cargar; sigue el número de ranura. */
    public static final String CARGADO_DESDE_RANURA = "Partida cargada de la ranura ";

    /** Título de la lista que se muestra para elegir la ranura que se carga. */
    public static final String RANURA_ELEGIR = "Elige la ranura que quieres cargar";

    /** Se muestra en la barra superior cuando todavía no hay partida. */
    public static final String NO_VALOR = "—";

    private static final Locale ESPANIOL = Locale.forLanguageTag("es");

    private Textos() {
                // contenedor de constantes
    }

    /** Etiqueta en español de una herramienta. */
    public static String herramienta(Herramienta herramienta) {
        return switch (herramienta) {
            case SOW -> "Sembrar";
            case WATER -> "Regar";
            case HARVEST -> "Cosechar";
            case BUILD -> "Construir";
            case FEED -> "Alimentar";
        };
    }

    /** Se muestra en la barra de estado cuando se desbloquea un logro. */
    public static String logroDesbloqueado(String name, int recompensa) {
        return "¡Logro desbloqueado: " + name + "! +" + dinero(recompensa);
    }

    /** Etiqueta de la casilla que activa y desactiva los efectos visuales y las animaciones. */
    public static final String EFECTOS = "Efectos";

    /**
     * Aviso que se muestra cuando empieza una estación nueva, por ejemplo {@code Comienza el Verano.} (HU-39 · CA-4).
     */
    public static String estacionIniciada(String idEstacion) {
        String nota = "SPRING".equals(idEstacion) ? "la" : "el";
        return "Comienza " + nota + " " + Nombres.estacion(idEstacion) + ".";
    }

    /** Tamaño de un edificio, por ejemplo {@code 2 × 2}. */
    public static String size(int width, int height) {
        return width + " × " + height;
    }

    /**
     * Nombre en español de un id de estación (por ejemplo {@code SPRING}); los ids desconocidos se devuelven sin
     * cambios.
     */
    public static String estacion(String id) {
        return Nombres.estacion(id);
    }

    /**
     * Nombre en español de un id de clima (por ejemplo {@code RAINY}); los ids desconocidos se devuelven sin cambios.
     */
    public static String clima(String id) {
        return Nombres.clima(id);
    }

    /** Dinero con separador de miles y el símbolo de moneda, por ejemplo {@code 1.250 m}. */
    /**
     * La tendencia de un precio como un símbolo y una palabra, de modo que el color nunca sea la única señal (HU-16 ·
     * CA-4).
     */
    public static String tendencia(String idTendencia) {
        return switch (idTendencia) {
            case "UP" -> "▲ sube";
            case "DOWN" -> "▼ baja";
            default -> "▬ igual";
        };
    }

    public static String dinero(int monto) {
        return String.format(ESPANIOL, "%,d m", monto);
    }

    /** La línea de la fecha, por ejemplo {@code Año 1 · Primavera · Día 1}. */
    public static String fecha(int anio, String idEstacion, int dia) {
        return "Año " + anio + " · " + estacion(idEstacion) + " · Día " + dia;
    }

    /** La línea del pronóstico, por ejemplo {@code Mañana: Lluvioso}. */
    public static String pronostico(String idClima) {
        return "Mañana: " + clima(idClima);
    }

    /** Energía como {@code actual/máxima}. */
    public static String energia(int actual, int max) {
        return actual + "/" + max;
    }
}
