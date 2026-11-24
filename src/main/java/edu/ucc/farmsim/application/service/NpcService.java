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
 * Hablar con las personas del valle y entregar sus encargos (RN-37, RN-38). Hablar necesita al avatar en una de las
 * cuatro parcelas junto a la persona, no cuesta energía y no cambia nada. Entregar toma los objetos, paga la recompensa
 * como ingreso del día y marca el encargo como hecho; nunca se paga dos veces.
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
     * Crea el servicio.
     *
     * @param session      la partida en curso
     * @param mapper       construye las vistas que dibuja la UI
     * @param dialogues    escribe lo que dice cada persona
     * @param events       donde se publican los eventos
     * @param achievements evalúa los logros después de una recompensa
     */
    public NpcService(GameSession session, ViewMapper mapper, DialogueService dialogues, EventBus events,
                      AchievementService achievements) {
        this.session = session;
        this.mapper = mapper;
        this.dialogues = dialogues;
        this.events = events;
        this.achievements = achievements;
    }

    /** Las personas del valle, dónde están y cómo van sus encargos. Una consulta pura. */
    public Result<List<NpcView>> npcs() {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        return Result.ok(mapper.npcs(session.current()));
    }

    /**
     * Lo que dice una persona, si el avatar está a su lado. Una consulta pura.
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
     * Entrega el encargo de una persona que está junto al avatar. Un encargo hecho se agradece y no se vuelve a pagar.
     *
     * @param npcId {@code MARTA}, {@code TOMAS}, {@code LUCIA} or {@code INES}
     * @return lo que dice la persona ahora; {@code ENCARGO_INCOMPLETO} si al inventario le faltan unidades
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
