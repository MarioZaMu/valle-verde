package edu.ucc.farmsim.dominio.edificio;

/**
 * Pattern: Builder — Role: Director (knows the order of the steps and which builder each type needs).
 *
 * <p>Stories: HU-18.
 */
public final class DirectorEdificio {

    private DirectorEdificio() {
                // solo métodos de utilidad estáticos
    }

    /**
     * Arma un edificio ejecutando en orden los pasos del constructor correspondiente.
     *
     * @param tipo tipo de edificio
     * @param id   posición del edificio en el orden en que se construyen
     * @param col  columna de su parcela superior izquierda
     * @param fila  fila de su parcela superior izquierda
     */
    public static Edificio armar(TipoEdificio tipo, int id, int col, int fila) {
        ConstructorEdificio builder = constructorPara(tipo);
        builder.colocarEn(id, col, fila);
        builder.alojamientoDefinir();
        builder.efectosDefinir();
        return builder.resultado();
    }

    private static ConstructorEdificio constructorPara(TipoEdificio tipo) {
        return switch (tipo) {
            case COOP -> new ConstructorGallinero();
            case BARN -> new ConstructorEstablo();
            case WAREHOUSE -> new ConstructorAlmacen();
            case GREENHOUSE -> new ConstructorInvernadero();
        };
    }
}
