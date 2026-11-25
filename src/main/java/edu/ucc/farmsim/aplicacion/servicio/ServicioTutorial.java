package edu.ucc.farmsim.aplicacion.servicio;

import edu.ucc.farmsim.aplicacion.dto.VistaTutorial;
import edu.ucc.farmsim.aplicacion.evento.BusEvento;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.dominio.progreso.PasoTutorial;
import edu.ucc.farmsim.dominio.progreso.Tutorial;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * El tutorial de inicio (RN-39): lo que dice el avatar en cada paso, avanzando cuando el jugador hizo la acción del
 * paso, y saltándolo. Solo guía: nunca cambia la energía, el dinero, el inventario ni el día.
 *
 * <p>Stories: HU-49.
 */
@Service
public final class ServicioTutorial {

    private static final Map<PasoTutorial, String> TEXTOS = new EnumMap<>(Map.of(
            PasoTutorial.WALK, "¡Hola! Soy tu granjero. Camina por la granja con las flechas o con W A S D.",
            PasoTutorial.SOW, "Elige Sembrar con la tecla 1, ponte frente a la tierra y pulsa Espacio "
                    + "(o haz clic en una parcela de tierra).",
            PasoTutorial.WATER, "Ahora elige Regar con la tecla 2 y riega lo que sembraste.",
            PasoTutorial.TALK, "Los vecinos viven en la aldea, al este por el camino. Acércate a uno y pulsa E.",
            PasoTutorial.END_DAY, "Cuando acabes, pulsa Enter o «Terminar día» para que crezcan tus cultivos.",
            PasoTutorial.DONE, "¡Listo! Ya sabes lo básico. ¡Buena suerte en el valle!",
            PasoTutorial.SKIPPED, "Tutorial saltado."));

    private final SesionJuego sesion;
    private final BusEvento eventos;

    /**
     * Crea el servicio.
     *
     * @param sesion la partida en curso
     * @param eventos  donde se publican los cambios de paso
     */
    public ServicioTutorial(SesionJuego sesion, BusEvento eventos) {
        this.sesion = sesion;
        this.eventos = eventos;
    }

    /** El tutorial de la partida en curso. Una consulta pura. */
    public Resultado<VistaTutorial> tutorial() {
        if (!sesion.tieneJuego()) {
            return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
        }
        return Resultado.ok(vista(sesion.actual().tutorial()));
    }

    /** Termina el tutorial de inmediato; saltar uno ya terminado es inofensivo y no publica nada. */
    public Resultado<VistaTutorial> saltar() {
        if (!sesion.tieneJuego()) {
            return Resultado.fallarCon(CodigoError.SIN_PARTIDA);
        }
        Tutorial tutorial = sesion.actual().tutorial();
        if (tutorial.saltar()) {
            eventos.publish(new EventoJuego.TutorialAvanzado(vista(tutorial)));
        }
        return Resultado.ok(vista(tutorial));
    }

    /**
     * El jugador hizo una acción aceptada: el tutorial avanza si era la acción del paso actual.
     *
     * @param accion el paso al que pertenece la acción
     */
    public void hecho(PasoTutorial accion) {
        if (!sesion.tieneJuego()) {
            return;
        }
        Tutorial tutorial = sesion.actual().tutorial();
        if (tutorial.complete(accion)) {
            eventos.publish(new EventoJuego.TutorialAvanzado(vista(tutorial)));
        }
    }

    private static VistaTutorial vista(Tutorial tutorial) {
        PasoTutorial paso = tutorial.paso();
        return new VistaTutorial(paso.name(), paso.numero(), PasoTutorial.CONTEO, TEXTOS.get(paso), paso.esConcluido());
    }
}
