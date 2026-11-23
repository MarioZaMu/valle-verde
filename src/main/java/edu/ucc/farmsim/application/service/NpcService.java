package edu.ucc.farmsim.application.service;

import edu.ucc.farmsim.application.dto.DialogueView;
import edu.ucc.farmsim.application.dto.NpcView;
import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Names;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.farm.Avatar;
import edu.ucc.farmsim.domain.game.GameState;
import edu.ucc.farmsim.domain.npc.Npc;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Talking to the people of the valley and delivering their errands (RN-37, RN-38). Talking needs the avatar on
 * one of the four plots next to the person, costs no energy and changes nothing. Delivering takes the items,
 * pays the reward as income of the day and marks the errand as done; it is never paid twice.
 *
 * <p>Stories: HU-46, HU-47.
 */
@Service
public final class NpcService {

    private final GameSession session;
    private final ViewMapper mapper;
    private final DialogueService dialogues;
    private final EventBus events;
    private final AchievementService achievements;

    /**
     * Creates the service.
     *
     * @param session      the game in progress
     * @param mapper       builds the views the UI draws
     * @param dialogues    writes what each person says
     * @param events       where events are published
     * @param achievements evaluates the achievements after a reward
     */
    public NpcService(GameSession session, ViewMapper mapper, DialogueService dialogues, EventBus events,
                      AchievementService achievements) {
        this.session = session;
        this.mapper = mapper;
        this.dialogues = dialogues;
        this.events = events;
        this.achievements = achievements;
    }

    /** The people of the valley, where they stand and how their errands are. A pure query. */
    public Result<List<NpcView>> npcs() {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        return Result.ok(mapper.npcs(session.current()));
    }

    /**
     * What a person says, if the avatar stands next to it. A pure query.
     *
     * @param npcId {@code MARTA}, {@code TOMAS}, {@code LUCIA} or {@code INES}
     */
    public Result<DialogueView> talk(String npcId) {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        Npc npc = Npc.valueOf(npcId);
        GameState state = session.current();
        if (!isNear(npc, state)) {
            return Result.failWith(ErrorCode.NPC_LEJOS, Names.npc(npc.name()));
        }
        return Result.ok(dialogues.dialogue(npc, state));
    }

    /**
     * Delivers the errand of a person standing next to the avatar. A done errand is thanked and not paid again.
     *
     * @param npcId {@code MARTA}, {@code TOMAS}, {@code LUCIA} or {@code INES}
     * @return what the person says now; {@code ENCARGO_INCOMPLETO} if the inventory lacks units
     */
    public Result<DialogueView> deliver(String npcId) {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        Npc npc = Npc.valueOf(npcId);
        GameState state = session.current();
        if (!isNear(npc, state)) {
            return Result.failWith(ErrorCode.NPC_LEJOS, Names.npc(npc.name()));
        }
        if (state.errands().isDone(npc)) {
            return Result.ok(dialogues.dialogue(npc, state));
        }
        int missing = npc.errandAmount() - state.inventory().count(npc.errandItem());
        if (missing > 0) {
            return Result.failWith(ErrorCode.ENCARGO_INCOMPLETO, Names.itemCount(npc.errandItem().name(), missing));
        }
        state.inventory().remove(npc.errandItem(), npc.errandAmount());
        state.earn(npc.errandReward());
        state.errands().complete(npc);
        events.publish(new GameEvent.InventoryChanged());
        events.publish(new GameEvent.MoneyChanged(state.money()));
        events.publish(new GameEvent.ErrandCompleted(npc.name(), Names.npc(npc.name()), npc.errandReward()));
        achievements.check(state);
        return Result.ok(dialogues.dialogue(npc, state));
    }

    private static boolean isNear(Npc npc, GameState state) {
        Avatar avatar = state.avatar();
        return npc.isNextTo(avatar.col(), avatar.row());
    }
}
