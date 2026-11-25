package edu.ucc.farmsim.aplicacion.evento;

import edu.ucc.farmsim.aplicacion.dto.VistaAvatar;
import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.dto.VistaResumenDia;
import edu.ucc.farmsim.aplicacion.dto.VistaTutorial;

/**
 * Algo que ocurrió en el juego y que la pantalla puede querer mostrar. Los eventos llevan vistas inmutables, nunca
 * objetos del dominio, de modo que la UI puede reaccionar sin importar el dominio.
 *
 * <p>Stories: HU-01, HU-10, HU-14, HU-15, HU-17, HU-26, HU-34, HU-45, HU-47, HU-49.
 */
public sealed interface EventoJuego {

    /** Se creó una partida nueva. */
    record JuegoIniciado(VistaHud hud) implements EventoJuego { }

    /** El día terminó; el HUD muestra la nueva fecha, el clima y la energía. */
    record DiaTerminado(VistaHud hud) implements EventoJuego { }

    /** Se desbloqueó un logro y se pagó su recompensa (RN-28). */
    record LogroDesbloqueado(String codigo, String name, int recompensa) implements EventoJuego { }

    /** El resumen del día que acaba de terminar (HU-17). */
    record ResumenDiaListo(VistaResumenDia resumen) implements EventoJuego { }

    /**
     * Algo cambió en el mapa de la granja (se sembró, regó o cosechó un cultivo, creció, se marchitó; se construyó un
     * edificio; llegó, comió o produjo un animal...).
     */
    record MapaCambiado() implements EventoJuego { }

    /** El dinero del jugador cambió (una compra o una venta). */
    record DineroCambiado(int dinero) implements EventoJuego { }

    /** Algo cambió en el inventario. */
    record InventarioCambiado() implements EventoJuego { }

    /** El avatar dio un paso (RN-36). */
    record AvatarMovido(VistaAvatar avatar) implements EventoJuego { }

    /** Se entregó y pagó un encargo de una persona del valle (RN-38). */
    record EncargoCompletado(String npcId, String name, int recompensa) implements EventoJuego { }

    /** El tutorial de inicio pasó a su siguiente paso, o terminó (RN-39). */
    record TutorialAvanzado(VistaTutorial tutorial) implements EventoJuego { }

    /** La energía cambió tras una acción. */
    record EnergiaCambiada(int energia, int energiaMaxima) implements EventoJuego { }
}
