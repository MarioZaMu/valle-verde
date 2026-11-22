package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.event.GameListener;
import edu.ucc.farmsim.ui.theme.Texts;

/**
 * Pattern: Observer — Role: ConcreteObserver (turns game events into notices).
 *
 * <p>It listens to the event bus: an unlocked achievement and the start of a new season become notices, and the
 * screen asks it directly for the errors and information that come from the player's own orders. It has no
 * Swing.
 *
 * <p>Stories: HU-26, HU-39.
 */
public final class ToastPresenter implements GameListener {

    private final ToastQueue queue;
    private String season;

    /**
     * Creates the presenter.
     *
     * @param queue where the notices are posted
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
        }
    }

    /** A rejection: the message of RN-35 as an error notice. */
    public void error(String message) {
        queue.post(ToastKind.ERROR, message);
    }

    /** Information for the player, such as a notice that came with a successful order. */
    public void info(String message) {
        queue.post(ToastKind.INFO, message);
    }
}
