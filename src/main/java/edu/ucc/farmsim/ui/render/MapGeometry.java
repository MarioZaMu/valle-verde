package edu.ucc.farmsim.ui.render;

import java.awt.Rectangle;

/**
 * Dónde se dibuja cada parcela: o el mapa completo ajustado a un área ({@link #fit}), o parcelas de tamaño fijo con una
 * cámara que sigue al avatar ({@link #follow}, RN-40). También responde qué parcela queda bajo un píxel. Sin ningún
 * componente Swing, así que se prueba sin pantalla.
 *
 * <p>Stories: HU-35, HU-44, HU-50.
 *
 * @param cols     número de columnas
 * @param rows     número de filas
 * @param cellSize lado de una celda en píxeles
 * @param originX  x de la esquina superior izquierda del mapa
 * @param originY  y de la esquina superior izquierda del mapa
 */
public record MapGeometry(int cols, int rows, int cellSize, int originX, int originY) {

    /** Lado en píxeles de un sprite: una celda que es múltiplo entero de él mantiene nítido el pixel art. */
    public static final int TILE = 32;

    /** Lado de una parcela en pantalla, en píxeles de pantalla, cuando la cámara sigue al avatar (RN-40). */
    public static final int SCREEN_CELL = 48;

    /** Parcelas a lo ancho y a lo alto que siempre caben: una ventana más pequeña recibe parcelas más pequeñas. */
    private static final int MIN_COLS_SHOWN = 12;
    private static final int MIN_ROWS_SHOWN = 8;

    /** Se usa una escala entera cuando llena al menos esta fracción (en cuartos) de lo que cabe. */
    private static final int WHOLE_SCALE_MIN_QUARTERS = 3;

    /**
     * Ajusta el mapa a un área, con las celdas cuadradas más grandes que caben, centrado. La celda es un múltiplo
     * entero de {@link #TILE} (una escala entera de los sprites) cuando eso desperdicia como mucho un cuarto del
     * espacio; si no, es la celda más grande que cabe, y los sprites se escalan por vecino más cercano.
     */
    public static MapGeometry fit(int cols, int rows, int width, int height) {
        int fitted = Math.max(1, Math.min(width / cols, height / rows));
        int whole = fitted / TILE * TILE;
        int size = whole >= TILE && whole * 4 >= fitted * WHOLE_SCALE_MIN_QUARTERS ? whole : fitted;
        int originX = (width - size * cols) / 2;
        int originY = (height - size * rows) / 2;
        return new MapGeometry(cols, rows, size, originX, originY);
    }

    /**
     * Ajusta el mapa a un área de la que se dejan libres una franja arriba y otra abajo (para las barras que flotan
     * sobre la vista); la tierra alrededor del mapa se sigue dibujando debajo de ellas.
     */
    public static MapGeometry fit(int cols, int rows, int width, int height, int top, int bottom) {
        MapGeometry inner = fit(cols, rows, width, Math.max(1, height - top - bottom));
        return new MapGeometry(cols, rows, inner.cellSize(), inner.originX(), inner.originY() + top);
    }

    /**
     * El lado de una parcela en píxeles lógicos: {@link #SCREEN_CELL} píxeles de pantalla (así que con Windows al 125 %
     * una parcela mide 38 píxeles lógicos), pero nunca tan grande que quepan menos de 12 x 8 parcelas en el área.
     *
     * @param deviceScale píxeles de pantalla por píxel lógico
     */
    public static int cellFor(int width, int height, double deviceScale) {
        int wanted = (int) Math.round(SCREEN_CELL / Math.max(deviceScale, 0.1));
        return Math.max(1, Math.min(wanted, Math.min(width / MIN_COLS_SHOWN, height / MIN_ROWS_SHOWN)));
    }

    /**
     * Parcelas del tamaño dado con una cámara sobre un punto del mapa: el punto va al centro de la vista, pero la
     * cámara se detiene en los bordes del mapa para que no se muestre nada fuera de él; en un eje donde cabe todo el
     * mapa, el mapa se centra en la vista.
     *
     * @param cell     lado de una parcela en píxeles
     * @param view     la parte del área que dejan libre las barras, donde debe quedar el punto
     * @param focusCol columna del punto, en parcelas; puede estar entre dos parcelas mientras el avatar camina
     * @param focusRow fila del punto, en parcelas
     */
    public static MapGeometry follow(int cols, int rows, int cell, Rectangle view, double focusCol, double focusRow) {
        int originX = axis(cols * cell, view.x, view.width, (focusCol + 0.5) * cell);
        int originY = axis(rows * cell, view.y, view.height, (focusRow + 0.5) * cell);
        return new MapGeometry(cols, rows, cell, originX, originY);
    }

    private static int axis(int length, int start, int size, double focus) {
        if (length <= size) {
            return start + (size - length) / 2;
        }
        int origin = (int) Math.round(start + size / 2.0 - focus);
        return Math.max(start + size - length, Math.min(start, origin));
    }

    /** Rectángulo en píxeles de todo el mapa. */
    public Rectangle area() {
        return new Rectangle(originX, originY, cols * cellSize, rows * cellSize);
    }

    /** Rectángulo en píxeles de una parcela. */
    public Rectangle cellBounds(int col, int row) {
        return new Rectangle(originX + col * cellSize, originY + row * cellSize, cellSize, cellSize);
    }

    /** La parcela bajo un píxel, o {@code null} si el píxel está fuera del mapa. */
    public CellPos cellAt(int x, int y) {
        int dx = x - originX;
        int dy = y - originY;
        if (dx < 0 || dy < 0 || dx >= cols * cellSize || dy >= rows * cellSize) {
            return null;
        }
        return new CellPos(dx / cellSize, dy / cellSize);
    }
}
