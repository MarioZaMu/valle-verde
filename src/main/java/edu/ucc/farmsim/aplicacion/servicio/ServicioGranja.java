package edu.ucc.farmsim.aplicacion.servicio;

import edu.ucc.farmsim.aplicacion.dto.VistaMapa;
import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
import edu.ucc.farmsim.aplicacion.evento.BusEvento;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Nombres;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import edu.ucc.farmsim.dominio.cultivo.Cultivo;
import edu.ucc.farmsim.dominio.cultivo.FabricaCultivo;
import edu.ucc.farmsim.dominio.cultivo.TipoCultivo;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.granja.MapaGranja;
import edu.ucc.farmsim.dominio.granja.Parcela;
import edu.ucc.farmsim.dominio.juego.Energia;
import edu.ucc.farmsim.dominio.juego.EstadoJuego;
import edu.ucc.farmsim.dominio.tiempo.Clima;
import edu.ucc.farmsim.dominio.tiempo.Estacion;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Los casos de uso de cultivos: sembrar, regar, cosechar y limpiar (RN-06 a RN-10). Cada operación comprueba primero
 * todas las reglas y solo después cambia la partida, de modo que un rechazo nunca deja nada a medias.
 *
 * <p>Stories: HU-01, HU-02, HU-04, HU-06, HU-07, HU-25, HU-26, HU-35.
 */
@Service
public final class ServicioGranja {

    private static final String SEPARADOR_ESTACION = " y ";

    private final SesionJuego sesion;
    private final MapeadorVista mapeador;
    private final BusEvento eventos;
    private final ServicioLogro logros;

    /**
     * Crea el servicio.
     *
     * @param sesion la partida en curso
     * @param mapeador  construye las vistas que dibuja la UI
     * @param eventos  donde se publican los eventos
     * @param logros evalúa los logros después de cada cosecha
     */
    public ServicioGranja(SesionJuego sesion, MapeadorVista mapeador, BusEvento eventos, ServicioLogro logros) {
        this.sesion = sesion;
        this.mapeador = mapeador;
        this.eventos = eventos;
        this.logros = logros;
    }

    /**
     * Siembra una semilla del inventario en una parcela de tierra libre de una estación válida (RN-06).
     *
     * @param col    columna de la parcela
     * @param fila    fila de la parcela
     * @param idSemilla id del objeto semilla, por ejemplo {@code CARROT_SEED}
     */
    public Resultado<VistaParcela> plantar(int col, int fila, String idSemilla) {
        if (!sesion.tieneJuego()) {
            return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
        }
        EstadoJuego estado = sesion.actual();
        MapaGranja granja = estado.granja();
        if (!granja.contains(col, fila) || !granja.parcelaEn(col, fila).puedeSerSembrado()) {
            return Resultado.fallarCon(CodigoError.PARCELA_NO_CULTIVABLE);
        }
        Parcela parcela = granja.parcelaEn(col, fila);
        if (parcela.tieneCultivo()) {
            return Resultado.fallarCon(CodigoError.PARCELA_OCUPADA);
        }
        TipoArticulo semilla = TipoArticulo.valueOf(idSemilla);
        if (semilla.categoria() != TipoArticulo.Categoria.SEED) {
            throw new IllegalArgumentException("Not a seed: " + idSemilla);
        }
        TipoCultivo tipo = semilla.tipoCultivo();
        Estacion estacion = estado.fecha().estacion();
        if (!parcela.esResguardado() && !tipo.estacionesValidos().contains(estacion)) {
            String valido = tipo.estacionesValidos().stream().map(s -> Nombres.estacion(s.name()))
                    .collect(Collectors.joining(SEPARADOR_ESTACION));
            return Resultado.fallarCon(CodigoError.FUERA_DE_ESTACION,
                    Nombres.cultivo(tipo.name()), Nombres.estacion(estacion.name()), valido);
        }
        if (estado.inventario().count(semilla) < ReglasJuego.SEMILLAS_POR_SIEMBRA) {
            return Resultado.fallarCon(CodigoError.SIN_SEMILLAS, Nombres.cultivo(tipo.name()));
        }
        Energia energia = estado.energia();
        if (!energia.puedeGastar(ReglasJuego.SEMBRAR_COSTO_ENERGIA)) {
            return Resultado.fallarCon(CodigoError.SIN_ENERGIA, ReglasJuego.SEMBRAR_COSTO_ENERGIA, energia.actual());
        }
        estado.inventario().remove(semilla, ReglasJuego.SEMILLAS_POR_SIEMBRA);
        energia.gastarIntentar(ReglasJuego.SEMBRAR_COSTO_ENERGIA);
        parcela.plantar(FabricaCultivo.paraTipo(tipo).cultivoCrear());
        cambiarPublicar(estado);
        eventos.publish(new EventoJuego.InventarioCambiado());
        return Resultado.ok(mapeador.parcela(estado, col, fila));
    }

    /**
     * Riega el cultivo de una parcela (RN-07): 1 de energía, o 2 cuando el clima del día es sequía.
     *
     * @param col columna de la parcela
     * @param fila fila de la parcela
     */
    public Resultado<VistaParcela> agua(int col, int fila) {
        if (!sesion.tieneJuego()) {
            return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
        }
        EstadoJuego estado = sesion.actual();
        Cultivo cultivo = cultivoEn(estado, col, fila);
        if (cultivo == null) {
            return Resultado.fallarCon(CodigoError.SIN_CULTIVO);
        }
        if (!cultivo.esVivo()) {
            return Resultado.fallarCon(CodigoError.CULTIVO_MARCHITO);
        }
        if (cultivo.esRegado()) {
            return Resultado.fallarCon(CodigoError.YA_REGADO);
        }
        boolean seco = estado.clima().hoy() == Clima.DROUGHT && !estado.granja().parcelaEn(col, fila).esResguardado();
        int costo = seco
                ? ReglasJuego.AGUA_COSTO_ENERGIA_EN_SEQUIA
                : ReglasJuego.AGUA_COSTO_ENERGIA;
        Energia energia = estado.energia();
        if (!energia.puedeGastar(costo)) {
            return Resultado.fallarCon(CodigoError.SIN_ENERGIA, costo, energia.actual());
        }
        cultivo.agua();
        energia.gastarIntentar(costo);
        cambiarPublicar(estado);
        return Resultado.ok(mapeador.parcela(estado, col, fila));
    }

    /**
     * La herramienta de cosechar (RN-10, RN-34): un cultivo maduro se cosecha hacia el inventario (2 de energía y lugar
     * para una unidad); un cultivo marchito se limpia (1 de energía, no se recoge nada). Si alguna regla falla, no
     * cambia nada; en particular un inventario lleno nunca cuesta el cultivo ni la energía.
     *
     * @param col columna de la parcela
     * @param fila fila de la parcela
     */
    public Resultado<VistaParcela> cosecha(int col, int fila) {
        if (!sesion.tieneJuego()) {
            return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
        }
        EstadoJuego estado = sesion.actual();
        Cultivo cultivo = cultivoEn(estado, col, fila);
        if (cultivo == null) {
            return Resultado.fallarCon(CodigoError.SIN_CULTIVO);
        }
        if (!cultivo.esVivo()) {
            return clear(estado, col, fila);
        }
        if (!cultivo.esMaduro()) {
            return Resultado.fallarCon(CodigoError.NO_MADURO, cultivo.izquierdaDias());
        }
        Energia energia = estado.energia();
        if (!energia.puedeGastar(ReglasJuego.COSECHAR_COSTO_ENERGIA)) {
            return Resultado.fallarCon(CodigoError.SIN_ENERGIA, ReglasJuego.COSECHAR_COSTO_ENERGIA, energia.actual());
        }
        if (!estado.inventario().add(TipoArticulo.producirDe(cultivo.tipo()), 1)) {
            return Resultado.fallarCon(CodigoError.INVENTARIO_LLENO);
        }
        energia.gastarIntentar(ReglasJuego.COSECHAR_COSTO_ENERGIA);
        estado.granja().parcelaEn(col, fila).cultivoQuitar();
        estado.progreso().cosecharRegistro();
        cambiarPublicar(estado);
        eventos.publish(new EventoJuego.InventarioCambiado());
        logros.comprobar(estado);
        return Resultado.ok(mapeador.parcela(estado, col, fila));
    }

    /**
     * Describe una parcela sin cambiar nada (HU-07): terreno, cultivo, etapa, regado y días restantes.
     *
     * @param col columna de la parcela
     * @param fila fila de la parcela
     */
    public Resultado<VistaParcela> inspeccionar(int col, int fila) {
        if (!sesion.tieneJuego()) {
            return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
        }
        return Resultado.ok(mapeador.parcela(sesion.actual(), col, fila));
    }

    /** Todo el mapa de la granja, para dibujarlo. Una consulta pura. */
    public Resultado<VistaMapa> mapa() {
        if (!sesion.tieneJuego()) {
            return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
        }
        return Resultado.ok(mapeador.map(sesion.actual()));
    }

    private Resultado<VistaParcela> clear(EstadoJuego estado, int col, int fila) {
        Energia energia = estado.energia();
        if (!energia.puedeGastar(ReglasJuego.COSTO_ENERGIA_LIMPIO)) {
            return Resultado.fallarCon(CodigoError.SIN_ENERGIA, ReglasJuego.COSTO_ENERGIA_LIMPIO, energia.actual());
        }
        energia.gastarIntentar(ReglasJuego.COSTO_ENERGIA_LIMPIO);
        estado.granja().parcelaEn(col, fila).cultivoQuitar();
        cambiarPublicar(estado);
        return Resultado.ok(mapeador.parcela(estado, col, fila));
    }

    private static Cultivo cultivoEn(EstadoJuego estado, int col, int fila) {
        MapaGranja granja = estado.granja();
        return granja.contains(col, fila) ? granja.parcelaEn(col, fila).cultivo() : null;
    }

    private void cambiarPublicar(EstadoJuego estado) {
        eventos.publish(new EventoJuego.MapaCambiado());
        eventos.publish(new EventoJuego.EnergiaCambiada(estado.energia().actual(), estado.energia().max()));
    }
}
