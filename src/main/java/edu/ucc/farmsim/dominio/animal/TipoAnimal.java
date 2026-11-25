package edu.ucc.farmsim.dominio.animal;

import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;

/**
 * Catálogo de animales (RN-23): lo que cuesta cada uno y el producto que da.
 *
 * <p>Stories: HU-19.
 */
public enum TipoAnimal {
    HEN(ReglasJuego.GALLINA_PRECIO, TipoArticulo.EGG),
    COW(ReglasJuego.VACA_PRECIO, TipoArticulo.MILK);

    private final int precio;
    private final TipoArticulo producto;

    TipoAnimal(int precio, TipoArticulo producto) {
        this.precio = precio;
        this.producto = producto;
    }

    /** Precio en m de un animal. */
    public int precio() {
        return precio;
    }

    /** Lo que produce el animal. */
    public TipoArticulo producto() {
        return producto;
    }
}
