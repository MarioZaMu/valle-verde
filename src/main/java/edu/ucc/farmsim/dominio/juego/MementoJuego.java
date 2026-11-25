package edu.ucc.farmsim.dominio.juego;

import edu.ucc.farmsim.dominio.economia.InstantaneaInventario;
import edu.ucc.farmsim.dominio.economia.InstantaneaMercado;
import edu.ucc.farmsim.dominio.granja.InstantaneaAvatar;
import edu.ucc.farmsim.dominio.granja.InstantaneaMapaGranja;
import edu.ucc.farmsim.dominio.npc.Npc;
import edu.ucc.farmsim.dominio.progreso.InstantaneaProgreso;
import edu.ucc.farmsim.dominio.progreso.PasoTutorial;
import edu.ucc.farmsim.dominio.tiempo.Clima;
import edu.ucc.farmsim.dominio.tiempo.FechaJuego;
import java.util.List;

/**
 * Pattern: Memento — Role: Memento (immutable snapshot of the game state).
 *
 * <p>Solo expone datos simples, así que el originador es el único lugar que sabe reconstruir un estado a partir de él.
 * Guarda todo lo que cambia durante el juego: cultivos, edificios y animales, el mercado, el libro diario, los logros,
 * el avatar, los encargos hechos y el paso del tutorial de inicio.
 *
 * <p>Stories: HU-01, HU-10, HU-15, HU-16, HU-17, HU-18, HU-26, HU-45, HU-47, HU-49, HU-50.
 *
 * @param fecha        fecha del calendario
 * @param dinero       monedas
 * @param energia      energía disponible
 * @param inventario   contenido del inventario
 * @param granja        cultivos del mapa
 * @param hoy       clima de hoy
 * @param pronostico    pronóstico de mañana
 * @param estadoAzar estado interno del generador aleatorio
 * @param mercado       demanda y precios anteriores
 * @param libro       lo que se ganó y se gastó hoy
 * @param progreso     los logros desbloqueados y sus contadores
 * @param avatar       dónde está el avatar y hacia dónde mira; {@code null} en una partida de versión 1, que no tenía
 *                     avatar
 * @param encargosHechos  las personas cuyo encargo está hecho; {@code null} en una partida escrita antes de los encargos
 * @param tutorial     el paso del tutorial de inicio; {@code null} en una partida escrita antes de él (se carga
 *                     terminado)
 */
public record MementoJuego(FechaJuego fecha, int dinero, int energia, InstantaneaInventario inventario,
                          InstantaneaMapaGranja granja, Clima hoy, Clima pronostico, long estadoAzar,
                          InstantaneaMercado mercado, InstantaneaLibro libro, InstantaneaProgreso progreso,
                          InstantaneaAvatar avatar, List<Npc> encargosHechos, PasoTutorial tutorial) {

    /** Una instantánea con el avatar y los encargos pero sin tutorial. */
    public MementoJuego(FechaJuego fecha, int dinero, int energia, InstantaneaInventario inventario, InstantaneaMapaGranja granja,
                       Clima hoy, Clima pronostico, long estadoAzar, InstantaneaMercado mercado,
                       InstantaneaLibro libro, InstantaneaProgreso progreso, InstantaneaAvatar avatar, List<Npc> encargosHechos) {
        this(fecha, dinero, energia, inventario, granja, hoy, pronostico, estadoAzar, mercado, libro, progreso, avatar,
                encargosHechos, null);
    }

    /** Una instantánea con el avatar pero sin encargos hechos. */
    public MementoJuego(FechaJuego fecha, int dinero, int energia, InstantaneaInventario inventario, InstantaneaMapaGranja granja,
                       Clima hoy, Clima pronostico, long estadoAzar, InstantaneaMercado mercado,
                       InstantaneaLibro libro, InstantaneaProgreso progreso, InstantaneaAvatar avatar) {
        this(fecha, dinero, energia, inventario, granja, hoy, pronostico, estadoAzar, mercado, libro, progreso, avatar,
                (List<Npc>) null);
    }

    /** Una instantánea sin avatar, como la guarda una partida de versión 1 (el avatar vuelve a su inicio). */
    public MementoJuego(FechaJuego fecha, int dinero, int energia, InstantaneaInventario inventario, InstantaneaMapaGranja granja,
                       Clima hoy, Clima pronostico, long estadoAzar, InstantaneaMercado mercado,
                       InstantaneaLibro libro, InstantaneaProgreso progreso) {
        this(fecha, dinero, energia, inventario, granja, hoy, pronostico, estadoAzar, mercado, libro, progreso, null);
    }

    /** La misma instantánea con otro mapa (una partida del antiguo mapa de 16 x 10 ampliada al valle, RN-40). */
    public MementoJuego conGranja(InstantaneaMapaGranja otro) {
        return new MementoJuego(fecha, dinero, energia, inventario, otro, hoy, pronostico, estadoAzar, mercado, libro,
                progreso, avatar, encargosHechos, tutorial);
    }
}
