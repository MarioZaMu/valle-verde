package edu.ucc.farmsim.aplicacion.dto;

/**
 * Lo que necesita la pantalla para dibujar y describir una parcela. Inmutable.
 *
 * <p>Stories: HU-01, HU-07, HU-25.
 *
 * @param col          columna de la parcela
 * @param fila          fila de la parcela
 * @param terreno      id del terreno ({@code SOIL}, {@code GRASS}, {@code WATER}, {@code OBSTACLE})
 * @param cultivo         id del cultivo (por ejemplo {@code CARROT}), o {@code null} si la parcela no tiene cultivo
 * @param etapa        id de la etapa de crecimiento ({@code SEED} a {@code WITHERED}), o {@code null} sin cultivo
 * @param regado      si el cultivo fue regado hoy
 * @param diasAMaduro días que aún faltan para madurar (0 cuando está maduro, marchito o sin cultivo)
 * @param resguardado    si un invernadero cubre la parcela, de modo que su cultivo está protegido (RN-22)
 */
public record VistaParcela(int col, int fila, String terreno, String cultivo, String etapa,
                       boolean regado, int diasAMaduro, boolean resguardado) {

    /** Una parcela a la intemperie: el caso común. */
    public VistaParcela(int col, int fila, String terreno, String cultivo, String etapa, boolean regado, int diasAMaduro) {
        this(col, fila, terreno, cultivo, etapa, regado, diasAMaduro, false);
    }
}
