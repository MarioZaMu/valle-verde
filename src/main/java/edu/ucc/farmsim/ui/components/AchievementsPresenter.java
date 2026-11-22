package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.dto.AchievementView;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.ui.theme.Texts;
import java.util.List;

/**
 * The logic of the achievements dialog, without any Swing (HU-26): one row per achievement with what it asks
 * for, its reward and whether it is unlocked or how far the player is.
 *
 * <p>Stories: HU-26.
 */
public final class AchievementsPresenter {

    private List<AchievementView> achievements = List.of();

    /**
     * Creates the presenter and reads the achievements.
     *
     * @param game the game the achievements belong to
     */
    public AchievementsPresenter(GameCommands game) {
        Result<List<AchievementView>> result = game.achievements();
        if (result.isOk()) {
            achievements = result.value();
        }
    }

    /** Every achievement. */
    public List<AchievementView> achievements() {
        return achievements;
    }

    /** The row of an achievement as the table shows it. */
    public Object[] row(AchievementView achievement) {
        return new Object[] {achievement.name(), achievement.description(), Texts.money(achievement.reward()),
                state(achievement)};
    }

    /** {@code Desbloqueado} or {@code progress/target}, so the state never depends on a colour. */
    public String state(AchievementView achievement) {
        return achievement.unlocked() ? Texts.UNLOCKED : achievement.progress() + "/" + achievement.target();
    }
}
