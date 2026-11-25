package edu.ucc.farmsim.aplicacion.servicio;

import edu.ucc.farmsim.aplicacion.dto.VistaLogro;
import edu.ucc.farmsim.aplicacion.evento.BusEvento;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Nombres;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import edu.ucc.farmsim.dominio.juego.EstadoJuego;
import edu.ucc.farmsim.dominio.progreso.Logro;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Evalúa los logros después de cada acción exitosa y anuncia los desbloqueados (RN-28), y los lista para el diálogo de
 * logros.
 *
 * <p>Stories: HU-26, HU-27.
 */
@Service
public final class ServicioLogro {

    private final SesionJuego sesion;
    private final BusEvento eventos;

    /**
     * Crea el servicio.
     *
     * @param sesion la partida en curso
     * @param eventos  donde se publican los eventos
     */
    public ServicioLogro(SesionJuego sesion, BusEvento eventos) {
        this.sesion = sesion;
        this.eventos = eventos;
    }

    /** Desbloquea lo que la partida merece ahora, paga las recompensas y publica lo ocurrido. */
    public void comprobar(EstadoJuego estado) {
        anunciar(estado, estado.logrosEvaluar());
    }

    /** Publica los eventos de los logros que se desbloquearon (por una acción o por el final del día). */
    public void anunciar(EstadoJuego estado, List<Logro> desbloqueado) {
        for (Logro logro : desbloqueado) {
            eventos.publish(new EventoJuego.LogroDesbloqueado(logro.codigo(), Nombres.logro(logro.name()),
                    logro.recompensa()));
        }
        if (!desbloqueado.isEmpty()) {
            eventos.publish(new EventoJuego.DineroCambiado(estado.dinero()));
        }
    }

    /** Todos los logros con el progreso del jugador. Una consulta pura. */
    public Resultado<List<VistaLogro>> logros() {
        if (!sesion.tieneJuego()) {
            return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
        }
        EstadoJuego estado = sesion.actual();
        var hechos = estado.hechos();
        List<VistaLogro> vistas = new ArrayList<>();
        for (Logro logro : Logro.values()) {
            int objetivo = objetivo(logro);
            int progreso = switch (logro) {
                case FIRST_HARVEST, GREEN_THUMB, BIG_HARVESTER -> hechos.cosechas();
                case TRADER -> hechos.unidadesVendidos();
                case CAPITALIST -> hechos.dinero();
                case BUILDER -> hechos.edificiosConstruidos();
                case BARNYARD_FARMER -> hechos.animales();
                case VETERINARIAN -> hechos.animalesCurados();
                case ONE_YEAR_IN_THE_VALLEY -> hechos.anio();
            };
            boolean desbloqueado = estado.progreso().has(logro);
            vistas.add(new VistaLogro(logro.codigo(), Nombres.logro(logro.name()),
                    Nombres.descripcionLogro(logro.name()), logro.recompensa(), desbloqueado,
                    desbloqueado ? objetivo : Math.min(progreso, objetivo), objetivo));
        }
        return Resultado.ok(List.copyOf(vistas));
    }

    private static int objetivo(Logro logro) {
        return switch (logro) {
            case FIRST_HARVEST, BUILDER, VETERINARIAN -> 1;
            case GREEN_THUMB -> ReglasJuego.MINIATURA_COSECHAS_VERDE;
            case BIG_HARVESTER -> ReglasJuego.COSECHADOR_COSECHAS_GRANDE;
            case TRADER -> ReglasJuego.COMERCIANTE_UNIDADES_VENDIDO;
            case CAPITALIST -> ReglasJuego.SALDO_CAPITALISTA;
            case BARNYARD_FARMER -> ReglasJuego.GRANJERO_CORRAL_ANIMALES;
            case ONE_YEAR_IN_THE_VALLEY -> ReglasJuego.ANIO_UNO_ANIO_EN_VALLE_EL;
        };
    }
}
