package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.dto.AchievementView;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.ui.theme.Texts;
import java.util.List;

/**
 * La lógica del diálogo de logros, sin nada de Swing (HU-26): una fila por logro con lo que pide, su recompensa y si
 * está desbloqueado o cuánto lleva el jugador.
 *
 * <p>Stories: HU-26.
 */
public final class AchievementsPresenter {

    private List<AchievementView> achievements = List.of();

    /**
     * Crea el presentador y lee los logros.
     *
     * @param game la partida a la que pertenecen los logros
     */
    public AchievementsPresenter(GameCommands game) {
        Result<List<AchievementView>> result = game.achievements();
        if (result.isOk()) {
            achievements = result.value();
        }
    }

    /** Todos los logros. */
    public List<AchievementView> achievements() {
        return achievements;
    }

    /** La fila de un logro tal como la muestra la tabla. */
    public Object[] row(AchievementView achievement) {
        return new Object[] {achievement.name(), achievement.description(), Texts.money(achievement.reward()),
                state(achievement)};
    }

    /** {@code Desbloqueado} o {@code progreso/meta}, de modo que el estado nunca depende de un color. */
    public String state(AchievementView achievement) {
        return achievement.unlocked() ? Texts.UNLOCKED : achievement.progress() + "/" + achievement.target();
    }
}
