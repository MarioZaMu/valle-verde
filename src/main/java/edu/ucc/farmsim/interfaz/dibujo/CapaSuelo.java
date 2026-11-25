package edu.ucc.farmsim.interfaz.dibujo;

import edu.ucc.farmsim.aplicacion.dto.VistaMapa;
import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
import edu.ucc.farmsim.interfaz.recursos.GestorRecurso;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * El suelo de la granja y de la tierra que la rodea, pintado al tamaño del arte (32 píxeles por baldosa) como una sola
 * imagen continua: pasto con algunas variantes elegidas por posición, tierra labrada, el camino del valle y agua (las
 * casas se levantan sobre pasto). Donde la tierra, el camino o el agua tocan el pasto, el pasto le crece un borde
 * irregular de píxeles encima y proyecta una sombra corta, de modo que las parcelas se leen como un solo campo y un
 * solo estanque en lugar de una grilla de cuadrados. La imagen solo cambia cuando cambia un terreno, una parcela
 * regada, la estación o el cuadro del agua, así que se construye una vez y se reutiliza cuadro tras cuadro.
 *
 * <p>Stories: HU-35, HU-44, HU-48, HU-50.
 */
final class CapaSuelo {

    /** Cuadros de la animación del agua. */
    static final int MARCOS_AGUA = 3;

    private static final int T = GeometriaMapa.BALDOSA;
    private static final int ENTRADAS_MAXIMOS = 4;
    private static final double SOMBRA_TIERRA = 0.70;
    private static final double SOMBRA_BANCO = 0.72;
    private static final int ESPUMA = 0xFF8EC6EE;

    private final GestorRecurso recursos;
    private final Map<String, BufferedImage> cache = new LinkedHashMap<>(16, 0.75f, true) {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> masAntiguo) {
            return size() > ENTRADAS_MAXIMOS;
        }
    };

    private int pinta;

    CapaSuelo(GestorRecurso recursos) {
        this.recursos = recursos;
    }

    /**
     * Verdadero cuando existen todos los sprites de suelo de la estación, de modo que se puede pintar el suelo
     * continuo.
     */
    boolean disponible(String estacion) {
        return recursos.sprite(nombreHierba(estacion, 0)).isPresent() && recursos.sprite("terrain/SOIL.png").isPresent()
                && recursos.sprite("terrain/SOIL_WATERED.png").isPresent() && recursos.sprite("terrain/WATER.png").isPresent();
    }

    /**
     * El suelo de las baldosas desde {@code (col0, row0)} (inclusive) hasta {@code (col1, row1)} (exclusive), donde las
     * baldosas fuera del mapa son pasto.
     */
    BufferedImage imagen(VistaMapa map, String estacion, int marco, int col0, int row0, int col1, int row1) {
        StringBuilder clave = new StringBuilder(map.parcelas().size() + 40);
        clave.append(estacion).append('|').append(marco).append('|').append(col0).append(',').append(row0).append(',')
                .append(col1).append(',').append(row1).append('|').append(map.cols()).append('|');
        for (VistaParcela parcela : map.parcelas()) {
            clave.append(parcela.terreno().charAt(0)).append(parcela.regado() ? '1' : '0');
        }
        return cache.computeIfAbsent(clave.toString(), k -> paint(map, estacion, marco, col0, row0, col1, row1));
    }

    /** Cuántas veces se pintó realmente el suelo (no se tomó de la caché). */
    int pinta() {
        return pinta;
    }

    private BufferedImage paint(VistaMapa map, String estacion, int marco, int col0, int row0, int col1, int row1) {
        pinta++;
        int cols = col1 - col0;
        int filas = row1 - row0;
        String[] terreno = terrenos(map);
        boolean[] regado = regado(map);
        BufferedImage fuera = new BufferedImage(cols * T, filas * T, BufferedImage.TYPE_INT_ARGB);
        int[] baldosa = new int[T * T];
        int[] hierba = new int[T * T];
        for (int fila = row0; fila < row1; fila++) {
            for (int col = col0; col < col1; col++) {
                String aqui = terrenoEn(terreno, map, col, fila);
                BufferedImage baldosaHierba = sprite(nombreHierba(estacion, variante(col, fila))).orElseGet(
                        () -> sprite(nombreHierba(estacion, 0)).orElseThrow());
                baldosaHierba.getRGB(0, 0, T, T, hierba, 0, T);
                if ("SOIL".equals(aqui) || "WATER".equals(aqui) || "PATH".equals(aqui)) {
                    String name = "WATER".equals(aqui) ? regarNombre(marco)
                            : "PATH".equals(aqui) ? nombreRuta()
                            : regado[fila * map.cols() + col] ? "terrain/SOIL_WATERED.png" : "terrain/SOIL.png";
                    BufferedImage base = sprite(name).orElseGet(() -> sprite("terrain/WATER.png").orElseThrow());
                    base.getRGB(0, 0, T, T, baldosa, 0, T);
                    bordes(baldosa, hierba, aqui, terreno, map, col, fila);
                } else {
                    System.arraycopy(hierba, 0, baldosa, 0, baldosa.length);
                }
                fuera.setRGB((col - col0) * T, (fila - row0) * T, T, T, baldosa, 0, T);
            }
        }
        return fuera;
    }

    /** El pasto crece sobre los lados de la tierra y el agua que tocan otra cosa, y proyecta una sombra corta. */
    private static void bordes(int[] baldosa, int[] hierba, String aqui, String[] terreno, VistaMapa map, int col, int fila) {
        boolean agua = "WATER".equals(aqui);
        int[][] sides = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        for (int lado = 0; lado < sides.length; lado++) {
            String next = terrenoEn(terreno, map, col + sides[lado][0], fila + sides[lado][1]);
            if (aqui.equals(next)) {
                continue;
            }
            for (int junto = 0; junto < T; junto++) {
                int global = lado < 2 ? col * T + junto : fila * T + junto;
                int profundidad = flequillo(global, lado);
                for (int k = 0; k < profundidad + 3; k++) {
                    int x = lado < 2 ? junto : (lado == 2 ? k : T - 1 - k);
                    int y = lado < 2 ? (lado == 0 ? k : T - 1 - k) : junto;
                    int i = y * T + x;
                    if (k < profundidad) {
                        baldosa[i] = hierba[i];
                    } else if (lado == 0 && k < profundidad + (agua ? 3 : 2)) {
                        baldosa[i] = oscurecer(baldosa[i], agua ? SOMBRA_BANCO : SOMBRA_TIERRA);
                    } else if (k == profundidad) {
                        baldosa[i] = agua ? ESPUMA : oscurecer(baldosa[i], 0.82);
                    }
                }
            }
        }
    }

    /** Cuántos píxeles alcanza el pasto sobre el borde en ese punto; irregular pero continuo entre baldosas. */
    private static int flequillo(int global, int lado) {
        int v = Math.floorMod(global * 37 + lado * 11 + (global / 5) * 3, 9);
        return v < 2 ? 4 : (v < 5 ? 3 : 2);
    }

    private static int oscurecer(int argb, double k) {
        int r = (int) (((argb >> 16) & 0xFF) * k);
        int g = (int) (((argb >> 8) & 0xFF) * k);
        int b = (int) ((argb & 0xFF) * k);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    /** Cuál de las cuatro baldosas de pasto cubre un lugar: sobre todo la lisa, a veces matas, flores o piedrecitas. */
    static int variante(int col, int fila) {
        int h = Math.floorMod(col * 73_856_093 ^ fila * 19_349_663, 20);
        return h < 12 ? 0 : (h < 16 ? 1 : (h < 18 ? 2 : 3));
    }

    static String nombreHierba(String estacion, int variante) {
        return "terrain/GRASS_" + estacion + (variante == 0 ? "" : "_" + variante) + ".png";
    }

    /** La tierra pisada del camino, o tierra lisa cuando el paquete no tiene baldosa de camino. */
    private String nombreRuta() {
        return sprite("terrain/PATH.png").isPresent() ? "terrain/PATH.png" : "terrain/SOIL.png";
    }

    static String regarNombre(int marco) {
        return marco == 0 ? "terrain/WATER.png" : "terrain/WATER_" + marco + ".png";
    }

    private Optional<BufferedImage> sprite(String name) {
        return recursos.sprite(name);
    }

    private static String[] terrenos(VistaMapa map) {
        String[] todo = new String[map.cols() * map.filas()];
        for (VistaParcela parcela : map.parcelas()) {
            String t = parcela.terreno();
            todo[parcela.fila() * map.cols() + parcela.col()] = "OBSTACLE".equals(t) || "HOUSE".equals(t) ? "GRASS" : t;
        }
        return todo;
    }

    private static boolean[] regado(VistaMapa map) {
        boolean[] todo = new boolean[map.cols() * map.filas()];
        for (VistaParcela parcela : map.parcelas()) {
            todo[parcela.fila() * map.cols() + parcela.col()] = parcela.regado();
        }
        return todo;
    }

    private static String terrenoEn(String[] terreno, VistaMapa map, int col, int fila) {
        if (col < 0 || fila < 0 || col >= map.cols() || fila >= map.filas()) {
            return "GRASS";
        }
        String t = terreno[fila * map.cols() + col];
        return t == null ? "GRASS" : t;
    }
}
