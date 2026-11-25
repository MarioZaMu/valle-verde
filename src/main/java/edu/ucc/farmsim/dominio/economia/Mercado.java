package edu.ucc.farmsim.dominio.economia;

import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import edu.ucc.farmsim.dominio.tiempo.Estacion;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * El mercado: por cuánto se vende cada producto, cómo baja su demanda al venderse y se recupera cada día (RN-17,
 * RN-18), su tendencia (RN-19) y el catálogo fijo de compras (RN-16).
 *
 * <p>Cada producto vendible (los cinco cultivos, huevos y leche) tiene su propia demanda entre 50 y 100 por ciento. El
 * precio de una unidad lo calcula la {@link EstrategiaPrecios} del producto con la demanda de ese momento.
 *
 * <p>Stories: HU-14, HU-15, HU-16.
 */
public final class Mercado {

    private final EstrategiaPrecios preciosCultivo = new PreciosCultivo();
    private final EstrategiaPrecios preciosPlano = new PreciosFijos();
    private final Map<TipoArticulo, Integer> demanda = new EnumMap<>(TipoArticulo.class);
    private final Map<TipoArticulo, Integer> precioAnterior = new EnumMap<>(TipoArticulo.class);

    private Mercado() {
        for (TipoArticulo articulo : articulosVendibles()) {
            demanda.put(articulo, ReglasJuego.DEMANDA_MAXIMA);
            precioAnterior.put(articulo, 0);
        }
    }

    /** Un mercado de partida nueva: demanda completa en todo y sin historial de precios. */
    public static Mercado estandar() {
        return new Mercado();
    }

    /** Si un producto se puede vender: cultivos, huevos y leche. */
    public static boolean esVendible(TipoArticulo articulo) {
        return articulo.categoria() == TipoArticulo.Categoria.CROP || articulo.categoria() == TipoArticulo.Categoria.ANIMAL_PRODUCT;
    }

    /** Todos los productos vendibles, en un orden estable. */
    public static List<TipoArticulo> articulosVendibles() {
        List<TipoArticulo> articulos = new ArrayList<>();
        for (TipoArticulo articulo : TipoArticulo.values()) {
            if (esVendible(articulo)) {
                articulos.add(articulo);
            }
        }
        return articulos;
    }

    /** Si un producto está en el catálogo fijo de compras: semillas, heno y medicina (RN-16). */
    public static boolean esComprable(TipoArticulo articulo) {
        return articulo.categoria() == TipoArticulo.Categoria.SEED || articulo.categoria() == TipoArticulo.Categoria.SUPPLY;
    }

    /** Todos los productos comprables, en un orden estable. */
    public static List<TipoArticulo> articulosComprables() {
        List<TipoArticulo> articulos = new ArrayList<>();
        for (TipoArticulo articulo : TipoArticulo.values()) {
            if (esComprable(articulo)) {
                articulos.add(articulo);
            }
        }
        return articulos;
    }

    /** El precio fijo en m de una unidad de un producto comprable. */
    public static int precioComprar(TipoArticulo articulo) {
        return switch (articulo) {
            case HAY -> ReglasJuego.HENO_PRECIO;
            case MEDICINE -> ReglasJuego.MEDICINA_PRECIO;
            default -> {
                if (articulo.categoria() != TipoArticulo.Categoria.SEED) {
                    throw new IllegalArgumentException("Not purchasable: " + articulo);
                }
                yield articulo.tipoCultivo().precioSemilla();
            }
        };
    }

    /** La demanda de un producto vendible, en porcentaje. */
    public int demandaDe(TipoArticulo articulo) {
        exigirVendible(articulo);
        return demanda.get(articulo);
    }

    /** El precio de la siguiente unidad de un producto con la demanda actual. */
    public int precioDe(TipoArticulo articulo, Estacion estacion) {
        return estrategiaPara(articulo).precioUnidad(articulo, estacion, demandaDe(articulo));
    }

    /**
     * Vende una unidad: devuelve su precio con la demanda actual y luego baja la demanda en 2 (nunca por debajo de 50).
     */
    public int unidadVender(TipoArticulo articulo, Estacion estacion) {
        int precio = precioDe(articulo, estacion);
        demanda.put(articulo, Math.max(ReglasJuego.DEMANDA_MINIMA, demandaDe(articulo) - ReglasJuego.SOLTAR_DEMANDA_POR_UNIDAD));
        return precio;
    }

    /** Lo que pagaría vender la cantidad dada, cada unidad con la demanda que encontraría. No cambia nada. */
    public int cotizar(TipoArticulo articulo, Estacion estacion, int cantidad) {
        int actual = demandaDe(articulo);
        int total = 0;
        for (int i = 0; i < cantidad; i++) {
            total += estrategiaPara(articulo).precioUnidad(articulo, estacion, actual);
            actual = Math.max(ReglasJuego.DEMANDA_MINIMA, actual - ReglasJuego.SOLTAR_DEMANDA_POR_UNIDAD);
        }
        return total;
    }

    /**
     * Paso 4 de RN-03: recuerda el precio actual de cada producto como el de ayer (RN-19) y luego recupera la demanda
     * en 5 (nunca por encima de 100, RN-18).
     */
    public void terminarDeDia(Estacion estacion) {
        for (TipoArticulo articulo : articulosVendibles()) {
            precioAnterior.put(articulo, precioDe(articulo, estacion));
            demanda.put(articulo, Math.min(ReglasJuego.DEMANDA_MAXIMA, demandaDe(articulo) + ReglasJuego.RECUPERACION_DEMANDA_POR_DIA));
        }
    }

    /** Si el precio subió, bajó o quedó igual desde el último fin de día (RN-19). */
    public Tendencia tendenciaDe(TipoArticulo articulo, Estacion estacion) {
        int antes = precioAnterior.get(articulo);
        if (antes == 0) {
            return Tendencia.SAME;
        }
        int now = precioDe(articulo, estacion);
        if (now > antes) {
            return Tendencia.UP;
        }
        return now < antes ? Tendencia.DOWN : Tendencia.SAME;
    }

    /** Una copia inmutable del mercado. */
    public InstantaneaMercado instantanea() {
        return new InstantaneaMercado(demanda, precioAnterior);
    }

    /** Deja el mercado como estaba cuando se tomó la instantánea. */
    public void restaurarDesde(InstantaneaMercado instantanea) {
        demanda.putAll(instantanea.demanda());
        precioAnterior.putAll(instantanea.precioAnterior());
    }

    private EstrategiaPrecios estrategiaPara(TipoArticulo articulo) {
        exigirVendible(articulo);
        return articulo.categoria() == TipoArticulo.Categoria.CROP ? preciosCultivo : preciosPlano;
    }

    private static void exigirVendible(TipoArticulo articulo) {
        if (!esVendible(articulo)) {
            throw new IllegalArgumentException("Not sellable: " + articulo);
        }
    }
}
