package edu.ucc.farmsim.dominio.economia;

import edu.ucc.farmsim.dominio.cultivo.TipoCultivo;

/**
 * Todo objeto que se puede guardar en el inventario (RN-15): 5 semillas, 5 cultivos, huevo, leche, heno y medicina.
 *
 * <p>Stories: HU-01, HU-13.
 */
public enum TipoArticulo {
    CARROT_SEED(Categoria.SEED, TipoCultivo.CARROT),
    POTATO_SEED(Categoria.SEED, TipoCultivo.POTATO),
    TOMATO_SEED(Categoria.SEED, TipoCultivo.TOMATO),
    CORN_SEED(Categoria.SEED, TipoCultivo.CORN),
    PUMPKIN_SEED(Categoria.SEED, TipoCultivo.PUMPKIN),
    CARROT(Categoria.CROP, TipoCultivo.CARROT),
    POTATO(Categoria.CROP, TipoCultivo.POTATO),
    TOMATO(Categoria.CROP, TipoCultivo.TOMATO),
    CORN(Categoria.CROP, TipoCultivo.CORN),
    PUMPKIN(Categoria.CROP, TipoCultivo.PUMPKIN),
    EGG(Categoria.ANIMAL_PRODUCT, null),
    MILK(Categoria.ANIMAL_PRODUCT, null),
    HAY(Categoria.SUPPLY, null),
    MEDICINE(Categoria.SUPPLY, null);

    /** Categoría general de un objeto. */
    public enum Categoria { SEED, CROP, ANIMAL_PRODUCT, SUPPLY }

    private final Categoria categoria;
    private final TipoCultivo tipoCultivo;

    TipoArticulo(Categoria categoria, TipoCultivo tipoCultivo) {
        this.categoria = categoria;
        this.tipoCultivo = tipoCultivo;
    }

    /** Categoría de este objeto. */
    public Categoria categoria() {
        return categoria;
    }

    /**
     * El cultivo del que este objeto es la semilla o la cosecha, o {@code null} para los objetos sin relación con
     * cultivos.
     */
    public TipoCultivo tipoCultivo() {
        return tipoCultivo;
    }

    /** El objeto semilla de un cultivo. */
    public static TipoArticulo semillaDe(TipoCultivo tipo) {
        return find(Categoria.SEED, tipo);
    }

    /** El objeto cosecha de un cultivo. */
    public static TipoArticulo producirDe(TipoCultivo tipo) {
        return find(Categoria.CROP, tipo);
    }

    private static TipoArticulo find(Categoria categoria, TipoCultivo tipo) {
        for (TipoArticulo articulo : values()) {
            if (articulo.categoria == categoria && articulo.tipoCultivo == tipo) {
                return articulo;
            }
        }
        throw new IllegalStateException("No " + categoria + " item for " + tipo);
    }
}

