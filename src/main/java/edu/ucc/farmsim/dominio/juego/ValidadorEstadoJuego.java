package edu.ucc.farmsim.dominio.juego;

import edu.ucc.farmsim.dominio.animal.InstantaneaAnimal;
import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import edu.ucc.farmsim.dominio.cultivo.InstantaneaCultivo;
import edu.ucc.farmsim.dominio.economia.Inventario;
import edu.ucc.farmsim.dominio.economia.Mercado;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.edificio.InstantaneaEdificio;
import edu.ucc.farmsim.dominio.edificio.TipoEdificio;
import edu.ucc.farmsim.dominio.granja.MapaGranja;
import edu.ucc.farmsim.dominio.progreso.HechosProgreso;
import edu.ucc.farmsim.dominio.progreso.InstantaneaProgreso;
import edu.ucc.farmsim.dominio.progreso.Logro;
import java.util.ArrayList;
import java.util.List;

/**
 * Comprueba las invariantes de RN-33 (paso 5) en una partida leída de un guardado, sin ninguna entrada/salida: dinero,
 * energía, inventario, cultivos, edificios, animales, mercado, logros y avatar deben tener sentido en conjunto. Una
 * partida que falla nunca se carga.
 *
 * <p>Stories: HU-31, HU-45, HU-47.
 */
public final class ValidadorEstadoJuego {

    private ValidadorEstadoJuego() {
                // solo métodos de utilidad estáticos
    }

    /**
     * Cada invariante que rompe la partida.
     *
     * @return una línea técnica (sin texto en español) por cada violación; vacía si la partida es válida
     */
    public static List<String> violaciones(MementoJuego memento) {
        List<String> encontrado = new ArrayList<>();
        if (memento == null) {
            encontrado.add("no game");
            return encontrado;
        }
        if (memento.dinero() < 0) {
            encontrado.add("money is negative: " + memento.dinero());
        }
        if (memento.energia() < 0 || memento.energia() > ReglasJuego.ENERGIA_MAXIMA) {
            encontrado.add("energy out of range: " + memento.energia());
        }
        if (memento.fecha() == null || memento.hoy() == null || memento.pronostico() == null) {
            encontrado.add("date or weather missing");
        }
        if (memento.inventario() == null || memento.granja() == null || memento.mercado() == null
                || memento.libro() == null || memento.progreso() == null) {
            encontrado.add("a part of the game is missing");
            return encontrado;
        }
        libroComprobar(memento, encontrado);
        edificiosComprobarYInventario(memento, encontrado);
        cultivosComprobar(memento, encontrado);
        mercadoComprobar(memento, encontrado);
        progresoComprobar(memento, encontrado);
        return encontrado;
    }

    private static void libroComprobar(MementoJuego memento, List<String> encontrado) {
        if (memento.libro().ingresos() < 0 || memento.libro().gastos() < 0) {
            encontrado.add("the daily ledger is negative");
        }
    }

    private static void edificiosComprobarYInventario(MementoJuego memento, List<String> encontrado) {
        MapaGranja map = MapaGranja.inicial();
        int almacenes = 0;
        int indice = 0;
        for (InstantaneaEdificio edificio : memento.granja().edificios()) {
            if (edificio.id() != indice++) {
                encontrado.add("building ids are not consecutive");
            }
            if (!map.puedeColocar(edificio.tipo(), edificio.col(), edificio.fila())) {
                encontrado.add("building outside the map, not on free grass or overlapping: " + edificio);
                continue;
            }
            map.colocar(edificio.tipo(), edificio.col(), edificio.fila());
            almacenes += edificio.tipo() == TipoEdificio.WAREHOUSE ? 1 : 0;
            animalesComprobar(edificio, encontrado);
        }
        avatarComprobar(memento, map, encontrado);
        encargosComprobar(memento, encontrado);
        var inventario = memento.inventario();
        int esperado = ReglasJuego.RANURAS_BASE_INVENTARIO + almacenes * ReglasJuego.RANURAS_EXTRA_ALMACEN;
        if (inventario.capacidad() != esperado || inventario.ranuras().size() != inventario.capacidad()) {
            encontrado.add("inventory capacity " + inventario.capacidad() + " does not match the warehouses (" + esperado + ")");
            return;
        }
        for (Inventario.PilaArticulo pila : inventario.ranuras()) {
            if (pila.monto() < 0 || pila.monto() > ReglasJuego.PILA_MAXIMA
                    || (pila.monto() > 0 && pila.tipo() == null)) {
                encontrado.add("invalid stack: " + pila);
            }
        }
    }

    /**
     * Una partida de versión 1 no tiene avatar; si lo tiene, debe estar en una parcela sobre la que pueda caminar
     * (RN-36).
     */
    private static void avatarComprobar(MementoJuego memento, MapaGranja map, List<String> encontrado) {
        var avatar = memento.avatar();
        if (avatar == null) {
            return;
        }
        if (avatar.mirada() == null || !map.esTransitable(avatar.col(), avatar.fila())) {
            encontrado.add("the avatar is not on a plot it can walk on: " + avatar);
        }
    }

    /**
     * Una partida escrita antes de los encargos no tiene ninguno; si no, ninguna persona aparece repetida ni vacía
     * (RN-38).
     */
    private static void encargosComprobar(MementoJuego memento, List<String> encontrado) {
        var hecho = memento.encargosHechos();
        if (hecho != null && (hecho.stream().anyMatch(java.util.Objects::isNull)
                || hecho.stream().distinct().count() != hecho.size())) {
            encontrado.add("the errands done are not a list of different people: " + hecho);
        }
    }

    private static void animalesComprobar(InstantaneaEdificio edificio, List<String> encontrado) {
        int capacidad = edificio.tipo().animalAlojado() == null ? 0 : ReglasJuego.ANIMALES_POR_EDIFICIO;
        if (edificio.animales().size() > capacidad) {
            encontrado.add("more animals than the capacity of " + edificio.tipo());
        }
        for (InstantaneaAnimal animal : edificio.animales()) {
            if (animal.tipo() != edificio.tipo().animalAlojado() || animal.condicion() == null
                    || animal.diasSinAlimentar() < 0 || animal.pendiente() < 0
                    || animal.pendiente() > ReglasJuego.MAXIMO_PENDIENTE_POR_ANIMAL) {
                encontrado.add("invalid animal in " + edificio.tipo() + ": " + animal);
            }
        }
    }

    private static void cultivosComprobar(MementoJuego memento, List<String> encontrado) {
        MapaGranja inicial = MapaGranja.inicial();
        for (InstantaneaEdificio edificio : memento.granja().edificios()) {
            if (inicial.puedeColocar(edificio.tipo(), edificio.col(), edificio.fila())) {
                inicial.colocar(edificio.tipo(), edificio.col(), edificio.fila());
            }
        }
        List<InstantaneaCultivo> cultivos = memento.granja().cultivos();
        if (cultivos.size() != inicial.cols() * inicial.filas()) {
            encontrado.add("the map has " + cultivos.size() + " plots");
            return;
        }
        for (int i = 0; i < cultivos.size(); i++) {
            InstantaneaCultivo cultivo = cultivos.get(i);
            if (cultivo == null) {
                continue;
            }
            if (!inicial.parcelaEn(i % inicial.cols(), i / inicial.cols()).puedeSerSembrado()) {
                encontrado.add("a crop on a plot that is neither soil nor a greenhouse plot: " + i);
            }
            if (cultivo.tipo() == null || cultivo.diasCrecidos() < 0 || cultivo.diasSinAgua() < 0) {
                encontrado.add("invalid crop at plot " + i);
            }
        }
    }

    private static void mercadoComprobar(MementoJuego memento, List<String> encontrado) {
        for (TipoArticulo articulo : Mercado.articulosVendibles()) {
            Integer demanda = memento.mercado().demanda().get(articulo);
            if (demanda == null || demanda < ReglasJuego.DEMANDA_MINIMA || demanda > ReglasJuego.DEMANDA_MAXIMA) {
                encontrado.add("demand out of range for " + articulo);
            }
        }
    }

    private static void progresoComprobar(MementoJuego memento, List<String> encontrado) {
        InstantaneaProgreso progreso = memento.progreso();
        if (progreso.cosechas() < 0 || progreso.unidadesVendidos() < 0 || progreso.edificiosConstruidos() < 0
                || progreso.animalesCurados() < 0) {
            encontrado.add("a counter is negative");
            return;
        }
        HechosProgreso contadores = new HechosProgreso(0, progreso.cosechas(), progreso.unidadesVendidos(),
                progreso.edificiosConstruidos(), 0, progreso.animalesCurados(), 0);
        for (Logro logro : progreso.desbloqueado()) {
            boolean contado = switch (logro) {
                case FIRST_HARVEST, GREEN_THUMB, BIG_HARVESTER, TRADER, BUILDER, VETERINARIAN ->
                        logro.esCumplidoPor(contadores);
                case CAPITALIST, BARNYARD_FARMER, ONE_YEAR_IN_THE_VALLEY -> true;
            };
            if (!contado) {
                encontrado.add("achievement " + logro.codigo() + " is unlocked but its counter does not allow it");
            }
        }
    }
}
