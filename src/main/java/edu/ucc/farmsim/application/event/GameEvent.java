package edu.ucc.farmsim.application.event;

import edu.ucc.farmsim.application.dto.AvatarView;
import edu.ucc.farmsim.application.dto.DaySummaryView;
import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.dto.TutorialView;

/**
 * Algo que ocurrió en el juego y que la pantalla puede querer mostrar. Los eventos llevan vistas inmutables, nunca
 * objetos del dominio, de modo que la UI puede reaccionar sin importar el dominio.
 *
 * <p>Stories: HU-01, HU-10, HU-14, HU-15, HU-17, HU-26, HU-34, HU-45, HU-47, HU-49.
 */
public sealed interface GameEvent {

    /** Se creó una partida nueva. */
    record GameStarted(HudView hud) implements GameEvent { }

    /** El día terminó; el HUD muestra la nueva fecha, el clima y la energía. */
    record DayEnded(HudView hud) implements GameEvent { }

    /** Se desbloqueó un logro y se pagó su recompensa (RN-28). */
    record AchievementUnlocked(String code, String name, int reward) implements GameEvent { }

    /** El resumen del día que acaba de terminar (HU-17). */
    record DaySummaryReady(DaySummaryView summary) implements GameEvent { }

    /**
     * Algo cambió en el mapa de la granja (se sembró, regó o cosechó un cultivo, creció, se marchitó; se construyó un
     * edificio; llegó, comió o produjo un animal...).
     */
    record MapChanged() implements GameEvent { }

    /** El dinero del jugador cambió (una compra o una venta). */
    record MoneyChanged(int money) implements GameEvent { }

    /** Algo cambió en el inventario. */
    record InventoryChanged() implements GameEvent { }

    /** El avatar dio un paso (RN-36). */
    record AvatarMoved(AvatarView avatar) implements GameEvent { }

    /** Se entregó y pagó un encargo de una persona del valle (RN-38). */
    record ErrandCompleted(String npcId, String name, int reward) implements GameEvent { }

    /** El tutorial de inicio pasó a su siguiente paso, o terminó (RN-39). */
    record TutorialAdvanced(TutorialView tutorial) implements GameEvent { }

    /** La energía cambió tras una acción. */
    record EnergyChanged(int energy, int maxEnergy) implements GameEvent { }
}
