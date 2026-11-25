package edu.ucc.farmsim.aplicacion.servicio;

import edu.ucc.farmsim.aplicacion.dto.VistaInventario;
import edu.ucc.farmsim.aplicacion.dto.VistaMercado;
import edu.ucc.farmsim.aplicacion.dto.VistaOfertaAnimal;
import edu.ucc.farmsim.aplicacion.dto.VistaOfertaComprar;
import edu.ucc.farmsim.aplicacion.dto.VistaOfertaVender;
import edu.ucc.farmsim.aplicacion.dto.VistaRanura;
import edu.ucc.farmsim.aplicacion.evento.BusEvento;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Nombres;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.dominio.animal.TipoAnimal;
import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import edu.ucc.farmsim.dominio.economia.Inventario;
import edu.ucc.farmsim.dominio.economia.Mercado;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.juego.EstadoJuego;
import edu.ucc.farmsim.dominio.tiempo.Estacion;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Los casos de uso de economía: comprar a precios fijos (RN-16), vender al precio dinámico (RN-17, RN-18), el total
 * previsto de una venta, y las vistas del mercado y el inventario. Todo rechazo deja la partida intacta.
 *
 * <p>Stories: HU-14, HU-15, HU-16, HU-19, HU-26, HU-37, HU-38.
 */
@Service
public final class ServicioMercado {

    private final SesionJuego sesion;
    private final BusEvento eventos;
    private final ServicioLogro logros;

    /**
     * Crea el servicio.
     *
     * @param sesion la partida en curso
     * @param eventos  donde se publican los eventos
     * @param logros evalúa los logros después de cada venta
     */
    public ServicioMercado(SesionJuego sesion, BusEvento eventos, ServicioLogro logros) {
        this.sesion = sesion;
        this.eventos = eventos;
        this.logros = logros;
    }

    /**
     * Compra unidades al precio fijo: la cantidad debe ser de 1 a 99, el jugador debe poder pagar el total y las
     * unidades deben caber en el inventario (RN-14, RN-15, RN-16).
     *
     * @return el total pagado en m
     */
    public Resultado<Integer> comprar(String idArticulo, int cantidad) {
        if (!sesion.tieneJuego()) {
            return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
        }
        TipoArticulo articulo = TipoArticulo.valueOf(idArticulo);
        if (!Mercado.esComprable(articulo)) {
            throw new IllegalArgumentException("Not purchasable: " + idArticulo);
        }
        if (cantidad < ReglasJuego.TRANSACCION_MINIMA || cantidad > ReglasJuego.TRANSACCION_MAXIMA) {
            return Resultado.fallarCon(CodigoError.CANTIDAD_INVALIDA, ReglasJuego.TRANSACCION_MAXIMA);
        }
        EstadoJuego estado = sesion.actual();
        int total = Mercado.precioComprar(articulo) * cantidad;
        if (total > estado.dinero()) {
            return Resultado.fallarCon(CodigoError.SIN_FONDOS, total - estado.dinero());
        }
        if (!estado.inventario().add(articulo, cantidad)) {
            return Resultado.fallarCon(CodigoError.INVENTARIO_LLENO);
        }
        estado.gastar(total);
        eventos.publish(new EventoJuego.DineroCambiado(estado.dinero()));
        eventos.publish(new EventoJuego.InventarioCambiado());
        return Resultado.ok(total);
    }

    /**
     * Vende unidades una por una, cada una al precio de la demanda que encuentra, que luego baja (RN-17, RN-18).
     *
     * @return el total recibido en m
     */
    public Resultado<Integer> vender(String idArticulo, int cantidad) {
        if (!sesion.tieneJuego()) {
            return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
        }
        EstadoJuego estado = sesion.actual();
        TipoArticulo articulo = vendible(idArticulo);
        int propio = estado.inventario().count(articulo);
        if (cantidad < ReglasJuego.TRANSACCION_MINIMA || cantidad > propio) {
            return Resultado.fallarCon(CodigoError.CANTIDAD_INVALIDA, propio);
        }
        Estacion estacion = estado.fecha().estacion();
        int total = 0;
        for (int i = 0; i < cantidad; i++) {
            total += estado.mercado().unidadVender(articulo, estacion);
        }
        estado.inventario().remove(articulo, cantidad);
        estado.ganar(total);
        estado.progreso().ventaRegistro(cantidad);
        eventos.publish(new EventoJuego.DineroCambiado(estado.dinero()));
        eventos.publish(new EventoJuego.InventarioCambiado());
        logros.comprobar(estado);
        return Resultado.ok(total);
    }

    /** Lo que pagaría vender la cantidad ahora mismo, sin vender (el «total previsto» de HU-38). */
    public Resultado<Integer> cotizar(String idArticulo, int cantidad) {
        if (!sesion.tieneJuego()) {
            return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
        }
        EstadoJuego estado = sesion.actual();
        TipoArticulo articulo = vendible(idArticulo);
        int propio = estado.inventario().count(articulo);
        if (cantidad < ReglasJuego.TRANSACCION_MINIMA || cantidad > propio) {
            return Resultado.fallarCon(CodigoError.CANTIDAD_INVALIDA, propio);
        }
        return Resultado.ok(estado.mercado().cotizar(articulo, estado.fecha().estacion(), cantidad));
    }

    /** El mercado tal como lo muestra el diálogo: precios, tendencias y el catálogo de compra. */
    public Resultado<VistaMercado> mercado() {
        if (!sesion.tieneJuego()) {
            return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
        }
        EstadoJuego estado = sesion.actual();
        Estacion estacion = estado.fecha().estacion();
        List<VistaOfertaVender> vende = new ArrayList<>();
        for (TipoArticulo articulo : Mercado.articulosVendibles()) {
            vende.add(new VistaOfertaVender(articulo.name(), Nombres.articulo(articulo.name()), estado.inventario().count(articulo),
                    estado.mercado().precioDe(articulo, estacion), estado.mercado().tendenciaDe(articulo, estacion).name()));
        }
        List<VistaOfertaComprar> compra = new ArrayList<>();
        for (TipoArticulo articulo : Mercado.articulosComprables()) {
            compra.add(new VistaOfertaComprar(articulo.name(), Nombres.articulo(articulo.name()), Mercado.precioComprar(articulo)));
        }
        List<VistaOfertaAnimal> animales = new ArrayList<>();
        for (TipoAnimal tipo : TipoAnimal.values()) {
            int libre = estado.granja().edificios().stream().filter(b -> b.especie() == tipo)
                    .mapToInt(b -> b.capacidadAnimal() - b.animales().size()).sum();
            animales.add(new VistaOfertaAnimal(tipo.name(), Nombres.animal(tipo.name()), tipo.precio(), libre));
        }
        return Resultado.ok(new VistaMercado(vende, compra, animales));
    }

    /** El inventario como una grilla. */
    public Resultado<VistaInventario> inventario() {
        if (!sesion.tieneJuego()) {
            return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
        }
        Inventario inventario = sesion.actual().inventario();
        List<VistaRanura> ranuras = new ArrayList<>();
        for (Inventario.PilaArticulo pila : inventario.pilas()) {
            ranuras.add(new VistaRanura(pila.tipo().name(), Nombres.articulo(pila.tipo().name()), pila.monto()));
        }
        return Resultado.ok(new VistaInventario(inventario.capacidad(), ranuras));
    }

    private static TipoArticulo vendible(String idArticulo) {
        TipoArticulo articulo = TipoArticulo.valueOf(idArticulo);
        if (!Mercado.esVendible(articulo)) {
            throw new IllegalArgumentException("Not sellable: " + idArticulo);
        }
        return articulo;
    }
}
