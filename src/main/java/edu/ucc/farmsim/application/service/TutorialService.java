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
 * The start tutorial (RN-39): what the avatar says at each step, moving on when the player did the action of the
 * step, and skipping it. It only guides: it never changes energy, money, the inventory or the day.
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
            TutorialStep.TALK, "Los vecinos dan consejos y encargos. Acércate a uno y pulsa E.",
            TutorialStep.END_DAY, "Cuando acabes, pulsa Enter o «Terminar día» para que crezcan tus cultivos.",
            TutorialStep.DONE, "¡Listo! Ya sabes lo básico. ¡Buena suerte en el valle!",
            TutorialStep.SKIPPED, "Tutorial saltado."));

    private final GameSession session;
    private final EventBus events;

    /**
     * Creates the service.
     *
     * @param session the game in progress
     * @param events  where the changes of step are published
     */
    public TutorialService(GameSession session, EventBus events) {
        this.session = session;
        this.events = events;
    }

    /** The tutorial of the game in progress. A pure query. */
    public Result<TutorialView> tutorial() {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        return Result.ok(view(session.current().tutorial()));
    }

    /** Ends the tutorial at once; skipping a finished one is harmless and publishes nothing. */
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
     * The player did an accepted action: the tutorial moves on if it was the action of the current step.
     *
     * @param action the step the action belongs to
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
