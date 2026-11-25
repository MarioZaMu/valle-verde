package edu.ucc.farmsim.interfaz.dibujo;

import java.awt.Rectangle;

/**
 * Dónde se dibuja cada parcela: o el mapa completo ajustado a un área ({@link #ajustar}), o parcelas de tamaño fijo con una
 * cámara que sigue al avatar ({@link #seguir}, RN-40). También responde qué parcela queda bajo un píxel. Sin ningún
 * componente Swing, así que se prueba sin pantalla.
 *
 * <p>Stories: HU-35, HU-44, HU-50.
 *
 * @param cols     número de columnas
 * @param filas     número de filas
 * @param tamanioCelda lado de una celda en píxeles
 * @param originX  x de la esquina superior izquierda del mapa
 * @param originY  y de la esquina superior izquierda del mapa
 */
public record GeometriaMapa(int cols, int filas, int tamanioCelda, int originX, int originY) {

    /** Lado en píxeles de un sprite: una celda que es múltiplo entero de él mantiene nítido el pixel art. */
    public static final int BALDOSA = 32;

    /** Lado de una parcela en pantalla, en píxeles de pantalla, cuando la cámara sigue al avatar (RN-40). */
    public static final int CELDA_PANTALLA = 48;

    /** Parcelas a lo ancho y a lo alto que siempre caben: una ventana más pequeña recibe parcelas más pequeñas. */
    private static final int COLS_MINIMOS_MOSTRADOS = 12;
    private static final int FILAS_MINIMOS_MOSTRADOS = 8;

    /** Se usa una escala entera cuando llena al menos esta fracción (en cuartos) de lo que cabe. */
    private static final int CUARTOS_ESCALA_ENTEROS_MINIMOS = 3;

    /**
     * Ajusta el mapa a un área, con las celdas cuadradas más grandes que caben, centrado. La celda es un múltiplo
     * entero de {@link #BALDOSA} (una escala entera de los sprites) cuando eso desperdicia como mucho un cuarto del
     * espacio; si no, es la celda más grande que cabe, y los sprites se escalan por vecino más cercano.
     */
    public static GeometriaMapa ajustar(int cols, int filas, int width, int height) {
        int ajustado = Math.max(1, Math.min(width / cols, height / filas));
        int entero = ajustado / BALDOSA * BALDOSA;
        int size = entero >= BALDOSA && entero * 4 >= ajustado * CUARTOS_ESCALA_ENTEROS_MINIMOS ? entero : ajustado;
        int originX = (width - size * cols) / 2;
        int originY = (height - size * filas) / 2;
        return new GeometriaMapa(cols, filas, size, originX, originY);
    }

    /**
     * Ajusta el mapa a un área de la que se dejan libres una franja arriba y otra abajo (para las barras que flotan
     * sobre la vista); la tierra alrededor del mapa se sigue dibujando debajo de ellas.
     */
    public static GeometriaMapa ajustar(int cols, int filas, int width, int height, int arriba, int inferior) {
        GeometriaMapa interior = ajustar(cols, filas, width, Math.max(1, height - arriba - inferior));
        return new GeometriaMapa(cols, filas, interior.tamanioCelda(), interior.originX(), interior.originY() + arriba);
    }

    /**
     * El lado de una parcela en píxeles lógicos: {@link #CELDA_PANTALLA} píxeles de pantalla (así que con Windows al 125 %
     * una parcela mide 38 píxeles lógicos), pero nunca tan grande que quepan menos de 12 x 8 parcelas en el área.
     *
     * @param escalaDispositivo píxeles de pantalla por píxel lógico
     */
    public static int celdaPara(int width, int height, double escalaDispositivo) {
        int querido = (int) Math.round(CELDA_PANTALLA / Math.max(escalaDispositivo, 0.1));
        return Math.max(1, Math.min(querido, Math.min(width / COLS_MINIMOS_MOSTRADOS, height / FILAS_MINIMOS_MOSTRADOS)));
    }

    /**
     * Parcelas del tamaño dado con una cámara sobre un punto del mapa: el punto va al centro de la vista, pero la
     * cámara se detiene en los bordes del mapa para que no se muestre nada fuera de él; en un eje donde cabe todo el
     * mapa, el mapa se centra en la vista.
     *
     * @param celda     lado de una parcela en píxeles
     * @param vista     la parte del área que dejan libre las barras, donde debe quedar el punto
     * @param colFoco columna del punto, en parcelas; puede estar entre dos parcelas mientras el avatar camina
     * @param filaFoco fila del punto, en parcelas
     */
    public static GeometriaMapa seguir(int cols, int filas, int celda, Rectangle vista, double colFoco, double filaFoco) {
        int originX = eje(cols * celda, vista.x, vista.width, (colFoco + 0.5) * celda);
        int originY = eje(filas * celda, vista.y, vista.height, (filaFoco + 0.5) * celda);
        return new GeometriaMapa(cols, filas, celda, originX, originY);
    }

    private static int eje(int length, int start, int size, double foco) {
        if (length <= size) {
            return start + (size - length) / 2;
        }
        int origen = (int) Math.round(start + size / 2.0 - foco);
        return Math.max(start + size - length, Math.min(start, origen));
    }

    /** Rectángulo en píxeles de todo el mapa. */
    public Rectangle area() {
        return new Rectangle(originX, originY, cols * tamanioCelda, filas * tamanioCelda);
    }

    /** Rectángulo en píxeles de una parcela. */
    public Rectangle limitesCelda(int col, int fila) {
        return new Rectangle(originX + col * tamanioCelda, originY + fila * tamanioCelda, tamanioCelda, tamanioCelda);
    }

    /** La parcela bajo un píxel, o {@code null} si el píxel está fuera del mapa. */
    public PosCelda celdaEn(int x, int y) {
        int dx = x - originX;
        int dy = y - originY;
        if (dx < 0 || dy < 0 || dx >= cols * tamanioCelda || dy >= filas * tamanioCelda) {
            return null;
        }
        return new PosCelda(dx / tamanioCelda, dy / tamanioCelda);
    }
}
