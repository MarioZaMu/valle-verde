package edu.ucc.farmsim.application.service;

import edu.ucc.farmsim.application.dto.TutorialView;
import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.progress.Tutorial;
import edu.ucc.farmsim.domain.progress.TutorialStep;
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
public final class TutorialService {

    private static final Map<TutorialStep, String> TEXTS = new EnumMap<>(Map.of(
            TutorialStep.WALK, "¡Hola! Soy tu granjero. Camina por la granja con las flechas o con W A S D.",
            TutorialStep.SOW, "Elige Sembrar con la tecla 1, ponte frente a la tierra y pulsa Espacio "
                    + "(o haz clic en una parcela de tierra).",
            TutorialStep.WATER, "Ahora elige Regar con la tecla 2 y riega lo que sembraste.",
            TutorialStep.TALK, "Los vecinos viven en la aldea, al este por el camino. Acércate a uno y pulsa E.",
            TutorialStep.END_DAY, "Cuando acabes, pulsa Enter o «Terminar día» para que crezcan tus cultivos.",
            TutorialStep.DONE, "¡Listo! Ya sabes lo básico. ¡Buena suerte en el valle!",
            TutorialStep.SKIPPED, "Tutorial saltado."));

    private final GameSession session;
    private final EventBus events;

    /**
     * Crea el servicio.
     *
     * @param session la partida en curso
     * @param events  donde se publican los cambios de paso
     */
    public TutorialService(GameSession session, EventBus events) {
        this.session = session;
        this.events = events;
    }

    /** El tutorial de la partida en curso. Una consulta pura. */
    public Result<TutorialView> tutorial() {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        return Result.ok(view(session.current().tutorial()));
    }

    /** Termina el tutorial de inmediato; saltar uno ya terminado es inofensivo y no publica nada. */
    public Result<TutorialView> skip() {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        Tutorial tutorial = session.current().tutorial();
        if (tutorial.skip()) {
            events.publish(new GameEvent.TutorialAdvanced(view(tutorial)));
        }
        return Result.ok(view(tutorial));
    }

    /**
     * El jugador hizo una acción aceptada: el tutorial avanza si era la acción del paso actual.
     *
     * @param action el paso al que pertenece la acción
     */
    public void done(TutorialStep action) {
        if (!session.hasGame()) {
            return;
        }
        Tutorial tutorial = session.current().tutorial();
        if (tutorial.complete(action)) {
            events.publish(new GameEvent.TutorialAdvanced(view(tutorial)));
        }
    }

    private static TutorialView view(Tutorial tutorial) {
        TutorialStep step = tutorial.step();
        return new TutorialView(step.name(), step.number(), TutorialStep.COUNT, TEXTS.get(step), step.isFinished());
    }
}
