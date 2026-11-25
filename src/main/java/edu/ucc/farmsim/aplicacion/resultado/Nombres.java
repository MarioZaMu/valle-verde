package edu.ucc.farmsim.aplicacion.resultado;

import java.util.Map;

/**
 * Nombres en español para mostrar de los ids que la aplicación entrega a la UI (estaciones, clima, cultivos, etapas de
 * crecimiento, edificios, animales, logros), para que los mensajes y las pantallas usen siempre las mismas palabras.
 * Los ids desconocidos se devuelven sin cambios.
 *
 * <p>Stories: HU-01, HU-07, HU-18, HU-46, HU-47.
 */
public final class Nombres {

    private static final Map<String, String> ESTACIONES = Map.of(
            "SPRING", "Primavera", "SUMMER", "Verano", "AUTUMN", "Otoño", "WINTER", "Invierno");
    private static final Map<String, String> CLIMAS = Map.of(
            "SUNNY", "Soleado", "CLOUDY", "Nublado", "RAINY", "Lluvioso",
            "DROUGHT", "Sequía", "FROST", "Helada", "SNOWY", "Nevado");
    private static final Map<String, String> CULTIVOS = Map.of(
            "CARROT", "Zanahoria", "POTATO", "Papa", "TOMATO", "Tomate", "CORN", "Maíz", "PUMPKIN", "Calabaza");
    private static final Map<String, String> ETAPAS = Map.of(
            "SEED", "Semilla", "SPROUT", "Brote", "GROWING", "Crecimiento", "MATURE", "Maduro", "WITHERED", "Marchito");

    private static final Map<String, String> ARTICULOS = Map.ofEntries(
            Map.entry("CARROT_SEED", "Semilla de Zanahoria"), Map.entry("POTATO_SEED", "Semilla de Papa"),
            Map.entry("TOMATO_SEED", "Semilla de Tomate"), Map.entry("CORN_SEED", "Semilla de Maíz"),
            Map.entry("PUMPKIN_SEED", "Semilla de Calabaza"), Map.entry("CARROT", "Zanahoria"),
            Map.entry("POTATO", "Papa"), Map.entry("TOMATO", "Tomate"), Map.entry("CORN", "Maíz"),
            Map.entry("PUMPKIN", "Calabaza"), Map.entry("EGG", "Huevo"), Map.entry("MILK", "Leche"),
            Map.entry("HAY", "Heno"), Map.entry("MEDICINE", "Medicina"));

    private static final Map<String, String> ARTICULOS_PLURALES = Map.ofEntries(
            Map.entry("CARROT", "Zanahorias"), Map.entry("POTATO", "Papas"), Map.entry("TOMATO", "Tomates"),
            Map.entry("CORN", "Mazorcas de Maíz"), Map.entry("PUMPKIN", "Calabazas"), Map.entry("EGG", "Huevos"),
            Map.entry("MILK", "Leches"), Map.entry("HAY", "Heno"), Map.entry("MEDICINE", "Medicinas"));

    private static final Map<String, String> NPCS = Map.of(
            "MARTA", "Marta", "TOMAS", "Don Tomás", "LUCIA", "Dra. Lucía", "INES", "Inés");
    private static final Map<String, String> NPC_ROLES = Map.of(
            "MARTA", "Vendedora del mercado", "TOMAS", "Granjero veterano", "LUCIA", "Veterinaria",
            "INES", "Vecina");

    private static final Map<String, String> EDIFICIOS = Map.of(
            "COOP", "Gallinero", "BARN", "Establo", "WAREHOUSE", "Almacén", "GREENHOUSE", "Invernadero");
    private static final Map<String, String> EDIFICIOS_PLURALES = Map.of(
            "COOP", "Gallineros", "BARN", "Establos", "WAREHOUSE", "Almacenes", "GREENHOUSE", "Invernaderos");
    private static final Map<String, String> ANIMALS = Map.of("HEN", "Gallina", "COW", "Vaca");
    private static final Map<String, String> ANIMALES_PLURALES = Map.of("HEN", "Gallinas", "COW", "Vacas");
    private static final Map<String, String> CONDICIONES = Map.of(
            "HEALTHY", "Sana", "HUNGRY", "Hambrienta", "SICK", "Enferma");
    private static final Map<String, String> ACHIEVEMENTS = Map.of(
            "FIRST_HARVEST", "Primera cosecha", "GREEN_THUMB", "Manos a la tierra",
            "BIG_HARVESTER", "Gran cosechador", "TRADER", "Comerciante", "CAPITALIST", "Capitalista",
            "BUILDER", "Constructor", "BARNYARD_FARMER", "Granjero de corral", "VETERINARIAN", "Veterinario",
            "ONE_YEAR_IN_THE_VALLEY", "Un año en el valle");

    private static final Map<String, String> TEXTOS_LOGRO = Map.of(
            "FIRST_HARVEST", "Cosecha 1 cultivo.", "GREEN_THUMB", "Cosecha 25 cultivos en total.",
            "BIG_HARVESTER", "Cosecha 100 cultivos en total.", "TRADER", "Vende 50 unidades en total.",
            "CAPITALIST", "Ten un saldo de 3.000 m o más.", "BUILDER", "Construye tu primer edificio.",
            "BARNYARD_FARMER", "Ten 4 animales a la vez.", "VETERINARIAN", "Cura a un animal enfermo.",
            "ONE_YEAR_IN_THE_VALLEY", "Llega al Año 2.");

    private static final Map<String, String> TERRENOS = Map.of(
            "SOIL", "Tierra de cultivo", "GRASS", "Hierba", "WATER", "Agua", "OBSTACLE", "Obstáculo");

    private Nombres() {
                // solo métodos de utilidad estáticos
    }

    /** Nombre de la estación, por ejemplo {@code SPRING} a {@code Primavera}. */
    public static String estacion(String id) {
        return ESTACIONES.getOrDefault(id, id);
    }

    /** Nombre del clima, por ejemplo {@code RAINY} a {@code Lluvioso}. */
    public static String clima(String id) {
        return CLIMAS.getOrDefault(id, id);
    }

    /** Nombre del cultivo, por ejemplo {@code CARROT} a {@code Zanahoria}. */
    public static String cultivo(String id) {
        return CULTIVOS.getOrDefault(id, id);
    }

    /** Nombre de la etapa de crecimiento, por ejemplo {@code SPROUT} a {@code Brote}. */
    public static String etapa(String id) {
        return ETAPAS.getOrDefault(id, id);
    }

    /** Nombre en español de un objeto (semilla, cosecha, producto animal o insumo). */
    public static String articulo(String id) {
        return ARTICULOS.getOrDefault(id, id);
    }

    /**
     * Una cantidad de un objeto, por ejemplo {@code 2 Zanahorias} o {@code 1 Zanahoria}. El heno no se cuenta en
     * unidades, así que conserva su nombre: {@code 2 Heno}.
     */
    public static String conteoArticulo(String id, int monto) {
        return monto + " " + (monto == 1 ? articulo(id) : ARTICULOS_PLURALES.getOrDefault(id, articulo(id)));
    }

    /** Nombre de una persona del valle, por ejemplo {@code TOMAS} a {@code Don Tomás}. */
    public static String npc(String id) {
        return NPCS.getOrDefault(id, id);
    }

    /** Lo que hace una persona del valle, por ejemplo {@code LUCIA} a {@code Veterinaria}. */
    public static String rolNpc(String id) {
        return NPC_ROLES.getOrDefault(id, id);
    }

    /** Nombre del edificio, por ejemplo {@code COOP} a {@code Gallinero}. */
    public static String edificio(String id) {
        return EDIFICIOS.getOrDefault(id, id);
    }

    /** Plural del nombre de un edificio, por ejemplo {@code COOP} a {@code Gallineros}. */
    public static String edificioPlural(String id) {
        return EDIFICIOS_PLURALES.getOrDefault(id, id);
    }

    /** Nombre del animal, por ejemplo {@code HEN} a {@code Gallina}. */
    public static String animal(String id) {
        return ANIMALS.getOrDefault(id, id);
    }

    /** Plural del nombre de un animal, por ejemplo {@code COW} a {@code Vacas}. */
    public static String animalPlural(String id) {
        return ANIMALES_PLURALES.getOrDefault(id, id);
    }

    /** Nombre de la condición de un animal, por ejemplo {@code SICK} a {@code Enferma}. */
    public static String condicion(String id) {
        return CONDICIONES.getOrDefault(id, id);
    }

    /** Nombre de un logro, por ejemplo {@code GREEN_THUMB} a {@code Manos a la tierra}. */
    public static String logro(String id) {
        return ACHIEVEMENTS.getOrDefault(id, id);
    }

    /** Lo que pide un logro, en español. */
    public static String descripcionLogro(String id) {
        return TEXTOS_LOGRO.getOrDefault(id, id);
    }

    /** Nombre del terreno, por ejemplo {@code SOIL} a {@code Tierra de cultivo}. */
    public static String terreno(String id) {
        return TERRENOS.getOrDefault(id, id);
    }
}

