package edu.ucc.farmsim.domain.config;

/**
 * Fuente única de las reglas de negocio numéricas (RN-01 a RN-35): sin números mágicos en otros lugares (RNF-12).
 *
 * <p>Los valores reflejan docs/REGLAS_DE_NEGOCIO.md; si una regla cambia allí, cambia solo aquí. Esta clase no depende
 * de nada (guarda primitivos), así que cualquier paquete del dominio puede usarla.
 *
 * <p>Stories: HU-01, HU-05, HU-08, HU-09, HU-11, HU-13, HU-28, HU-33.
 */
public final class GameRules {

        // ---- RN-01: calendario ----

    /** Días de cada estación; un año de cuatro estaciones dura 112 días. */
    public static final int DAYS_PER_SEASON = 28;

    /** Año en que empieza una partida nueva. */
    public static final int START_YEAR = 1;

    /** Día de la estación en que empieza una partida nueva. */
    public static final int START_DAY = 1;

        // ---- RN-11: probabilidades del clima (porcentaje, en el orden del enum Weather) ---- Orden: SUNNY, CLOUDY, RAINY,
    // DROUGHT, FROST, SNOWY. Cada tabla suma 100.

    /** Tabla del clima de primavera. */
    public static final int[] WEATHER_WEIGHTS_SPRING = {55, 15, 30, 0, 0, 0};

    /** Tabla del clima de verano. */
    public static final int[] WEATHER_WEIGHTS_SUMMER = {60, 10, 15, 15, 0, 0};

    /** Tabla del clima de otoño para los días 1 a 21. */
    public static final int[] WEATHER_WEIGHTS_AUTUMN_EARLY = {50, 20, 30, 0, 0, 0};

    /** Tabla del clima de otoño para los días 22 a 28 (la helada se vuelve posible). */
    public static final int[] WEATHER_WEIGHTS_AUTUMN_LATE = {35, 15, 25, 0, 25, 0};

    /** Tabla del clima de invierno. */
    public static final int[] WEATHER_WEIGHTS_WINTER = {25, 20, 0, 0, 15, 40};

    /** Primer día de otoño en que es posible la helada. */
    public static final int AUTUMN_FROST_FROM_DAY = 22;

    /** Total de cada tabla del clima: el sorteo se hace entre 0 y este valor. */
    public static final int WEATHER_TABLE_TOTAL = 100;

        // ---- RN-02: energía diaria ----

    /** Energía máxima (e inicial). */
    public static final int MAX_ENERGY = 100;

    /** Costo de energía de sembrar. */
    public static final int ENERGY_COST_SOW = 2;

    /** Costo de energía de regar un cultivo en un día normal. */
    public static final int ENERGY_COST_WATER = 1;

    /** Costo de energía de regar un cultivo cuando el clima es sequía. */
    public static final int ENERGY_COST_WATER_IN_DROUGHT = 2;

    /** Costo de energía de cosechar un cultivo maduro. */
    public static final int ENERGY_COST_HARVEST = 2;

    /** Costo de energía de limpiar una parcela marchita. */
    public static final int ENERGY_COST_CLEAN = 1;

    /** Costo de energía de alimentar a cada animal. */
    public static final int ENERGY_COST_FEED_PER_ANIMAL = 1;

    /** Costo de energía de recoger los productos de un edificio. */
    public static final int ENERGY_COST_COLLECT = 1;

    /** Costo de energía de curar a un animal. */
    public static final int ENERGY_COST_CURE = 1;

        // ---- RN-04: el valle (48 x 24 = 1152 parcelas): la granja al oeste, la aldea al este ----

    /** Número de columnas de parcelas. */
    public static final int MAP_COLS = 48;

    /** Número de filas de parcelas. */
    public static final int MAP_ROWS = 24;

    /** Columnas del mapa antes de la fase 5.7g; las partidas de versión 1 y 2 guardan esa cantidad (RN-40). */
    public static final int LEGACY_MAP_COLS = 16;

    /** Filas del mapa antes de la fase 5.7g. */
    public static final int LEGACY_MAP_ROWS = 10;

    /** Columna de la parcela superior izquierda del bloque de tierra (8 x 5 = 40 parcelas de tierra). */
    public static final int SOIL_COL = 4;

    /** Fila de la parcela superior izquierda del bloque de tierra. */
    public static final int SOIL_ROW = 2;

    /** Ancho del bloque de tierra. */
    public static final int SOIL_WIDTH = 8;

    /** Alto del bloque de tierra. */
    public static final int SOIL_HEIGHT = 5;

    /** Columna de la parcela superior izquierda del estanque (3 x 2 = 6 parcelas de agua). */
    public static final int POND_COL = 13;

    /** Fila de la parcela superior izquierda del estanque. */
    public static final int POND_ROW = 0;

    /** Ancho del estanque. */
    public static final int POND_WIDTH = 3;

    /** Alto del estanque. */
    public static final int POND_HEIGHT = 2;

    /**
     * Posiciones fijas {columna, fila} de los 30 obstáculos (árboles y rocas): 12 en la granja antigua, 18 alrededor.
     */
    public static final int[][] OBSTACLES = {
        {0, 0}, {1, 0}, {0, 1}, {8, 0}, {0, 5}, {15, 5}, {0, 9}, {1, 9}, {7, 9}, {14, 9}, {15, 9}, {15, 8},
        {21, 1}, {22, 4}, {20, 9}, {23, 17}, {21, 21}, {10, 15}, {3, 13}, {14, 19}, {6, 20},
        {25, 3}, {33, 4}, {44, 6}, {46, 10}, {41, 19}, {31, 21}, {45, 22}, {26, 20}, {40, 2}
    };

    /** El camino de tierra {columna, fila, ancho, alto}: desde tu casa hacia el sur y luego al este por la aldea. */
    public static final int[][] PATHS = {{18, 6, 1, 7}, {19, 12, 27, 1}};

    /**
     * Las casas {columna, fila, ancho, alto} en el orden de {@code House}: tu casa en la granja, luego la tienda de
     * Marta, la casa de Don Tomás, la clínica de la Dra. Lucía y la casa de Inés en la aldea.
     */
    public static final int[][] HOUSES = {{16, 3, 4, 3}, {27, 8, 5, 3}, {36, 8, 4, 3}, {27, 14, 5, 3}, {36, 14, 4, 3}};

        // ---- RN-36: avatar ----

    /** Columna de la parcela de pasto donde está el avatar de una partida nueva, justo al sur de la tierra. */
    public static final int AVATAR_START_COL = 7;

    /** Fila de la parcela de pasto donde está el avatar de una partida nueva. */
    public static final int AVATAR_START_ROW = 7;

        // ---- RN-37, RN-38: las personas del valle y sus encargos ----

    /** Columna de Marta, la vendedora del mercado, frente a su tienda. */
    public static final int NPC_MARTA_COL = 29;

    /** Fila de Marta, justo al sur de su tienda y al norte del camino. */
    public static final int NPC_MARTA_ROW = 11;

    /** Columna de Don Tomás, el granjero veterano, frente a su casa. */
    public static final int NPC_TOMAS_COL = 37;

    /** Fila de Don Tomás. */
    public static final int NPC_TOMAS_ROW = 11;

    /** Columna de la Dra. Lucía, la veterinaria, frente a su clínica. */
    public static final int NPC_LUCIA_COL = 29;

    /** Fila de la Dra. Lucía, justo al sur de su clínica, en su puerta. */
    public static final int NPC_LUCIA_ROW = 17;

    /** Columna de Inés, la vecina, frente a su casa. */
    public static final int NPC_INES_COL = 37;

    /** Fila de Inés. */
    public static final int NPC_INES_ROW = 17;

    /** Zanahorias que pide Marta. */
    public static final int ERRAND_MARTA_AMOUNT = 5;

    /** Lo que paga Marta, en m. */
    public static final int ERRAND_MARTA_REWARD = 60;

    /** Papas que pide Don Tomás. */
    public static final int ERRAND_TOMAS_AMOUNT = 3;

    /** Lo que paga Don Tomás, en m. */
    public static final int ERRAND_TOMAS_REWARD = 50;

    /** Raciones de heno que pide la Dra. Lucía. */
    public static final int ERRAND_LUCIA_AMOUNT = 2;

    /** Lo que paga la Dra. Lucía, en m. */
    public static final int ERRAND_LUCIA_REWARD = 30;

    /** Huevos que pide Inés. */
    public static final int ERRAND_INES_AMOUNT = 4;

    /** Lo que paga Inés, en m. */
    public static final int ERRAND_INES_REWARD = 80;

        // ---- RN-30: partida nueva ----

    /** Dinero al inicio de una partida. */
    public static final int START_MONEY = 500;

    /** Semillas de zanahoria en el inventario inicial. */
    public static final int START_CARROT_SEEDS = 10;

    /** Semillas de papa en el inventario inicial. */
    public static final int START_POTATO_SEEDS = 5;

        // ---- RN-16, RN-17, RN-18: mercado ----

    /** Precio en m de una ración de heno. */
    public static final int PRICE_HAY = 5;

    /** Precio en m de una medicina. */
    public static final int PRICE_MEDICINE = 40;

    /** Precio base de venta en m de un huevo. */
    public static final int BASE_PRICE_EGG = 20;

    /** Precio base de venta en m de una botella de leche. */
    public static final int BASE_PRICE_MILK = 60;

    /** Factor de estación, en porcentaje, de un cultivo vendido en una de sus estaciones válidas. */
    public static final int SEASON_FACTOR_IN_SEASON = 90;

    /** Factor de estación, en porcentaje, de un cultivo vendido fuera de sus estaciones válidas. */
    public static final int SEASON_FACTOR_OUT_OF_SEASON = 120;

    /** Factor de estación, en porcentaje, de los productos animales. */
    public static final int SEASON_FACTOR_FLAT = 100;

    /** Cantidad más pequeña de una transacción. */
    public static final int MIN_TRANSACTION = 1;

    /** Cantidad más grande de una compra. */
    public static final int MAX_TRANSACTION = 99;

    /** Demanda de cada producto cuando empieza una partida, y su techo, en porcentaje. */
    public static final int DEMAND_MAX = 100;

    /** Demanda más baja a la que puede caer un producto, en porcentaje. */
    public static final int DEMAND_MIN = 50;

    /** Demanda que pierde cada unidad vendida. */
    public static final int DEMAND_DROP_PER_UNIT = 2;

    /** Demanda que se recupera cuando termina un día. */
    public static final int DEMAND_RECOVERY_PER_DAY = 5;

        // ---- RN-31: ranuras de guardado ----

    /** Número de ranuras de guardado. */
    public static final int SAVE_SLOTS = 3;

        // ---- RN-06: siembra ----

    /** Semillas que consume una siembra. */
    public static final int SEEDS_PER_SOWING = 1;

        // ---- RN-09: marchitamiento ----

    /** Días consecutivos sin agua después de los cuales se marchita un cultivo joven. */
    public static final int DRY_DAYS_TO_WITHER = 3;

    /** Lo mismo, cuando el clima del día que termina es sequía. */
    public static final int DRY_DAYS_TO_WITHER_IN_DROUGHT = 2;

    /** Lo mismo dentro de un invernadero, sea cual sea el clima: protege sus cultivos de la sequía (RN-22). */
    public static final int DRY_DAYS_TO_WITHER_IN_GREENHOUSE = 3;

        // ---- RN-15: inventario ----

    /** Número base de casillas de inventario. */
    public static final int INVENTORY_BASE_SLOTS = 24;

    /** Número máximo de unidades en una pila. */
    public static final int MAX_STACK = 99;

        // ---- RN-20, RN-21, RN-22: edificios ----

    /** Costo en m de un gallinero. */
    public static final int COOP_COST = 400;

    /** Mayor número de gallineros. */
    public static final int COOP_MAX = 2;

    /** Costo en m de un establo. */
    public static final int BARN_COST = 1000;

    /** Mayor número de establos. */
    public static final int BARN_MAX = 2;

    /** Costo en m de un almacén. */
    public static final int WAREHOUSE_COST = 300;

    /** Mayor número de almacenes. */
    public static final int WAREHOUSE_MAX = 1;

    /** Casillas de inventario que agrega un almacén. */
    public static final int WAREHOUSE_EXTRA_SLOTS = 12;

    /** Costo en m de un invernadero. */
    public static final int GREENHOUSE_COST = 1500;

    /** Mayor número de invernaderos. */
    public static final int GREENHOUSE_MAX = 1;

    /** Parcelas que protege un invernadero (3 x 3). */
    public static final int GREENHOUSE_PROTECTED_PLOTS = 9;

        // ---- RN-28: logros ----

    /** Recompensa en m de LG-01, primera cosecha. */
    public static final int REWARD_FIRST_HARVEST = 50;

    /** Cosechas que otorgan LG-02. */
    public static final int HARVESTS_GREEN_THUMB = 25;

    /** Recompensa en m de LG-02. */
    public static final int REWARD_GREEN_THUMB = 150;

    /** Cosechas que otorgan LG-03. */
    public static final int HARVESTS_BIG_HARVESTER = 100;

    /** Recompensa en m de LG-03. */
    public static final int REWARD_BIG_HARVESTER = 400;

    /** Unidades vendidas que otorgan LG-04. */
    public static final int UNITS_SOLD_TRADER = 50;

    /** Recompensa en m de LG-04. */
    public static final int REWARD_TRADER = 100;

    /** Saldo en m que otorga LG-05. */
    public static final int BALANCE_CAPITALIST = 3000;

    /** Recompensa en m de LG-05. */
    public static final int REWARD_CAPITALIST = 200;

    /** Recompensa en m de LG-06, primer edificio. */
    public static final int REWARD_BUILDER = 100;

    /** Animales a la vez que otorgan LG-07. */
    public static final int ANIMALS_BARNYARD_FARMER = 4;

    /** Recompensa en m de LG-07. */
    public static final int REWARD_BARNYARD_FARMER = 150;

    /** Recompensa en m de LG-08, primer animal curado. */
    public static final int REWARD_VETERINARIAN = 80;

    /** Año que otorga LG-09. */
    public static final int YEAR_ONE_YEAR_IN_THE_VALLEY = 2;

    /** Recompensa en m de LG-09. */
    public static final int REWARD_ONE_YEAR_IN_THE_VALLEY = 500;

        // ---- RN-23 a RN-27: animales ----

    /** Animales que puede alojar un gallinero o un establo. */
    public static final int ANIMALS_PER_BUILDING = 4;

    /** Precio en m de una gallina. */
    public static final int PRICE_HEN = 150;

    /** Precio en m de una vaca. */
    public static final int PRICE_COW = 800;

    /** Heno que come cada animal por día. */
    public static final int HAY_PER_ANIMAL = 1;

    /** Días consecutivos sin comer después de los cuales un animal se enferma. */
    public static final int DAYS_WITHOUT_FEED_TO_SICK = 3;

    /** Unidades de producto que puede tener un animal esperando ser recogidas. */
    public static final int MAX_PENDING_PER_ANIMAL = 2;

    /** Medicinas que usa una curación. */
    public static final int MEDICINE_PER_CURE = 1;

    private GameRules() {
                // contenedor de constantes
    }
}
