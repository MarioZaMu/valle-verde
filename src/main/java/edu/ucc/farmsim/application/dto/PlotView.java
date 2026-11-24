package edu.ucc.farmsim.application.dto;

/**
 * Lo que necesita la pantalla para dibujar y describir una parcela. Inmutable.
 *
 * <p>Stories: HU-01, HU-07, HU-25.
 *
 * @param col          columna de la parcela
 * @param row          fila de la parcela
 * @param terrain      id del terreno ({@code SOIL}, {@code GRASS}, {@code WATER}, {@code OBSTACLE})
 * @param crop         id del cultivo (por ejemplo {@code CARROT}), o {@code null} si la parcela no tiene cultivo
 * @param stage        id de la etapa de crecimiento ({@code SEED} a {@code WITHERED}), o {@code null} sin cultivo
 * @param watered      si el cultivo fue regado hoy
 * @param daysToMature días que aún faltan para madurar (0 cuando está maduro, marchito o sin cultivo)
 * @param sheltered    si un invernadero cubre la parcela, de modo que su cultivo está protegido (RN-22)
 */
public record PlotView(int col, int row, String terrain, String crop, String stage,
                       boolean watered, int daysToMature, boolean sheltered) {

    /** Una parcela a la intemperie: el caso común. */
    public PlotView(int col, int row, String terrain, String crop, String stage, boolean watered, int daysToMature) {
        this(col, row, terrain, crop, stage, watered, daysToMature, false);
    }
}
