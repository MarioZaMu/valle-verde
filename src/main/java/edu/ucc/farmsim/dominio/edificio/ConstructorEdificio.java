package edu.ucc.farmsim.dominio.edificio;

/**
 * Pattern: Builder — Role: Builder (declares the steps that assemble a building).
 *
 * <p>El {@link DirectorEdificio} llama a los pasos en un orden fijo; cada constructor concreto decide cuál es el
 * alojamiento y los efectos de su edificio. Aquí nunca ocurren rechazos: el caso de uso comprueba todas las reglas de
 * RN-21 antes de pedir un edificio.
 *
 * <p>Stories: HU-18.
 */
public abstract class ConstructorEdificio {

    private int id;
    private int col;
    private int fila;

    /** El tipo de edificio que arma este constructor. */
    abstract TipoEdificio tipo();

    /** Paso 1: dónde está y cuál es. */
    final void colocarEn(int idEdificio, int colIzquierdaArriba, int filaIzquierdaArriba) {
        this.id = idEdificio;
        this.col = colIzquierdaArriba;
        this.fila = filaIzquierdaArriba;
    }

    /** Paso 2: cuántos animales viven dentro y de qué especie (especie {@code null}: ninguno). */
    abstract void alojamientoDefinir();

    /** Paso 3: qué más hace el edificio (más casillas de inventario, cultivos protegidos). */
    abstract void efectosDefinir();

    /** La capacidad de alojamiento decidida en el paso 2. */
    abstract int capacidadAnimal();

    /** Las casillas de inventario decididas en el paso 3. */
    abstract int ranurasInventarioExtra();

    /** Las parcelas protegidas decididas en el paso 3. */
    abstract int parcelasProtegidos();

    /** Paso final: el edificio terminado. */
    final Edificio resultado() {
        return new Edificio(id, tipo(), col, fila, tipo().animalAlojado(), capacidadAnimal(),
                ranurasInventarioExtra(), parcelasProtegidos());
    }
}
