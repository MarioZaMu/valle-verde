package edu.ucc.farmsim.application.service;

import edu.ucc.farmsim.application.dto.AchievementView;
import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Names;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.game.GameState;
import edu.ucc.farmsim.domain.progress.Achievement;
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
public final class AchievementService {

    private final GameSession session;
    private final EventBus events;

    /**
     * Crea el servicio.
     *
     * @param session la partida en curso
     * @param events  donde se publican los eventos
     */
    public AchievementService(GameSession session, EventBus events) {
        this.session = session;
        this.events = events;
    }

    /** Desbloquea lo que la partida merece ahora, paga las recompensas y publica lo ocurrido. */
    public void check(GameState state) {
        announce(state, state.evaluateAchievements());
    }

    /** Publica los eventos de los logros que se desbloquearon (por una acción o por el final del día). */
    public void announce(GameState state, List<Achievement> unlocked) {
        for (Achievement achievement : unlocked) {
            events.publish(new GameEvent.AchievementUnlocked(achievement.code(), Names.achievement(achievement.name()),
                    achievement.reward()));
        }
        if (!unlocked.isEmpty()) {
            events.publish(new GameEvent.MoneyChanged(state.money()));
        }
    }

    /** Todos los logros con el progreso del jugador. Una consulta pura. */
    public Result<List<AchievementView>> achievements() {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        GameState state = session.current();
        var facts = state.facts();
        List<AchievementView> views = new ArrayList<>();
        for (Achievement achievement : Achievement.values()) {
            int target = target(achievement);
            int progress = switch (achievement) {
                case FIRST_HARVEST, GREEN_THUMB, BIG_HARVESTER -> facts.harvests();
                case TRADER -> facts.unitsSold();
                case CAPITALIST -> facts.money();
                case BUILDER -> facts.buildingsBuilt();
                case BARNYARD_FARMER -> facts.animals();
                case VETERINARIAN -> facts.animalsCured();
                case ONE_YEAR_IN_THE_VALLEY -> facts.year();
            };
            boolean unlocked = state.progress().has(achievement);
            views.add(new AchievementView(achievement.code(), Names.achievement(achievement.name()),
                    Names.achievementDescription(achievement.name()), achievement.reward(), unlocked,
                    unlocked ? target : Math.min(progress, target), target));
        }
        return Result.ok(List.copyOf(views));
    }

    private static int target(Achievement achievement) {
        return switch (achievement) {
            case FIRST_HARVEST, BUILDER, VETERINARIAN -> 1;
            case GREEN_THUMB -> GameRules.HARVESTS_GREEN_THUMB;
            case BIG_HARVESTER -> GameRules.HARVESTS_BIG_HARVESTER;
            case TRADER -> GameRules.UNITS_SOLD_TRADER;
            case CAPITALIST -> GameRules.BALANCE_CAPITALIST;
            case BARNYARD_FARMER -> GameRules.ANIMALS_BARNYARD_FARMER;
            case ONE_YEAR_IN_THE_VALLEY -> GameRules.YEAR_ONE_YEAR_IN_THE_VALLEY;
        };
    }
}
