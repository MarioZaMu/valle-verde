package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.event.GameListener;
import edu.ucc.farmsim.ui.theme.Texts;

/**
 * Pattern: Observer — Role: ConcreteObserver (turns game events into notices).
 *
 * <p>Escucha el bus de eventos: un logro desbloqueado y el comienzo de una estación nueva se vuelven avisos, y la
 * pantalla le pide directamente los errores y la información que vienen de las órdenes del propio jugador. No tiene
 * Swing.
 *
 * <p>Stories: HU-26, HU-39, HU-47, HU-49.
 */
public final class ToastPresenter implements GameListener {

    private final ToastQueue queue;
    private String season;

    /**
     * Crea el presentador.
     *
     * @param queue donde se publican los avisos
     */
    public ToastPresenter(ToastQueue queue) {
        this.queue = queue;
    }

    @Override
    public void onEvent(GameEvent event) {
        if (event instanceof GameEvent.GameStarted started) {
            season = started.hud().season();
        } else if (event instanceof GameEvent.DayEnded ended) {
            String now = ended.hud().season();
            if (season != null && !season.equals(now)) {
                queue.post(ToastKind.INFO, Texts.seasonStarted(now));
            }
            season = now;
        } else if (event instanceof GameEvent.AchievementUnlocked unlocked) {
            queue.post(ToastKind.ACHIEVEMENT, Texts.achievementUnlocked(unlocked.name(), unlocked.reward()));
        } else if (event instanceof GameEvent.ErrandCompleted errand) {
            queue.post(ToastKind.INFO, Texts.errandCompleted(errand.name(), errand.reward()));
        } else if (event instanceof GameEvent.TutorialAdvanced advanced && "DONE".equals(advanced.tutorial().step())) {
            queue.post(ToastKind.INFO, advanced.tutorial().text());
        }
    }

    /** Un rechazo: el mensaje de RN-35 como aviso de error. */
    public void error(String message) {
        queue.post(ToastKind.ERROR, message);
    }

    /** Información para el jugador, como un aviso que vino con una orden exitosa. */
    public void info(String message) {
        queue.post(ToastKind.INFO, message);
    }
}
