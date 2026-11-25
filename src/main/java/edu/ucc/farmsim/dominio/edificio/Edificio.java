package edu.ucc.farmsim.dominio.edificio;

import edu.ucc.farmsim.dominio.animal.Animal;
import edu.ucc.farmsim.dominio.animal.InstantaneaAnimal;
import edu.ucc.farmsim.dominio.animal.TipoAnimal;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pattern: Builder — Role: Product (the building the builders assemble step by step).
 *
 * <p>Un edificio colocado en el mapa. Un gallinero o un establo aloja animales y guarda lo que producen hasta que el
 * jugador lo recoge (RN-26); un almacén amplía el inventario; un invernadero protege cultivos.
 *
 * <p>Stories: HU-18, HU-19, HU-20, HU-22, HU-24.
 */
public final class Edificio {

    private final int id;
    private final TipoEdificio tipo;
    private final int col;
    private final int fila;
    private final TipoAnimal especie;
    private final int capacidadAnimal;
    private final int ranurasInventarioExtra;
    private final int parcelasProtegidos;
    private final List<Animal> animales = new ArrayList<>();

    Edificio(int id, TipoEdificio tipo, int col, int fila, TipoAnimal especie, int capacidadAnimal,
             int ranurasInventarioExtra, int parcelasProtegidos) {
        this.id = id;
        this.tipo = tipo;
        this.col = col;
        this.fila = fila;
        this.especie = especie;
        this.capacidadAnimal = capacidadAnimal;
        this.ranurasInventarioExtra = ranurasInventarioExtra;
        this.parcelasProtegidos = parcelasProtegidos;
    }

    /** Reconstruye un edificio y sus animales desde una instantánea. */
    public static Edificio desdeInstantanea(InstantaneaEdificio instantanea) {
        Edificio edificio = DirectorEdificio.armar(instantanea.tipo(), instantanea.id(), instantanea.col(), instantanea.fila());
        for (InstantaneaAnimal animal : instantanea.animales()) {
            edificio.animales.add(Animal.desdeInstantanea(animal));
        }
        return edificio;
    }

    /** Una copia inmutable de este edificio. */
    public InstantaneaEdificio instantanea() {
        return new InstantaneaEdificio(id, tipo, col, fila, animales.stream().map(Animal::instantanea).toList());
    }

    /** Posición en el orden en que se construyeron los edificios; lo identifica. */
    public int id() {
        return id;
    }

    /** Tipo de edificio. */
    public TipoEdificio tipo() {
        return tipo;
    }

    /** Columna de la parcela superior izquierda. */
    public int col() {
        return col;
    }

    /** Fila de la parcela superior izquierda. */
    public int fila() {
        return fila;
    }

    /** Columnas que cubre. */
    public int width() {
        return tipo.width();
    }

    /** Filas que cubre. */
    public int height() {
        return tipo.height();
    }

    /** Verdadero si la parcela pertenece al edificio. */
    public boolean cubre(int colParcela, int filaParcela) {
        return colParcela >= col && colParcela < col + width() && filaParcela >= fila && filaParcela < fila + height();
    }

    /** El animal que aloja este edificio, o {@code null}. */
    public TipoAnimal especie() {
        return especie;
    }

    /** Cuántos animales caben. */
    public int capacidadAnimal() {
        return capacidadAnimal;
    }

    /** Casillas de inventario que agrega este edificio. */
    public int ranurasInventarioExtra() {
        return ranurasInventarioExtra;
    }

    /** Parcelas de cultivo que protege este edificio. */
    public int parcelasProtegidos() {
        return parcelasProtegidos;
    }

    /** Los animales que hay dentro, en el orden en que llegaron. */
    public List<Animal> animales() {
        return Collections.unmodifiableList(animales);
    }

    /** Verdadero si cabe un animal más del tipo (RN-23). */
    public boolean tieneEspacioPara(TipoAnimal animal) {
        return especie == animal && animales.size() < capacidadAnimal;
    }

    /**
     * Aloja a un animal.
     *
     * @return {@code true} si cupo; {@code false} (y nada cambia) si el edificio aloja otra especie o está lleno
     */
    public boolean animalAgregar(Animal animal) {
        if (!tieneEspacioPara(animal.tipo())) {
            return false;
        }
        animales.add(animal);
        return true;
    }

    /** Unidades de producto que esperan ser recogidas, de todos los animales (RN-26). */
    public int productosPendientes() {
        return animales.stream().mapToInt(Animal::pendiente).sum();
    }

    /** Lo que producen los animales de este edificio, o {@code null} si no aloja ninguno. */
    public TipoArticulo producto() {
        return especie == null ? null : especie.producto();
    }

    /** Toma todas las unidades que esperan ser recogidas. */
    public int todoRecolectar() {
        int total = 0;
        for (Animal animal : animales) {
            total += animal.collect();
        }
        return total;
    }
}
