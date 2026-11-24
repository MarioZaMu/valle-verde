package edu.ucc.farmsim.domain.save;

import edu.ucc.farmsim.domain.game.GameMemento;
import java.time.Instant;
import java.util.List;

/**
 * Puerto que define el dominio y que implementa la infraestructura: dónde se guardan las partidas (inversión de
 * dependencias, DT-11). Todo fallo técnico del almacenamiento es una {@link SaveStorageException}; una ranura que no se
 * puede convertir en partida es una {@link SaveLoadException}.
 *
 * <p>Stories: HU-29, HU-30, HU-31, HU-32, HU-33.
 */
public interface SaveRepository {

    /**
     * Si una ranura contiene una partida guardada.
     *
     * @param slot número de ranura, de 1 al número de ranuras
     */
    boolean slotExists(int slot);

    /**
     * Escribe la partida en la ranura en una sola operación atómica: el contenido anterior se reemplaza o, si falla la
     * escritura, queda como estaba (RN-31).
     *
     * @param slot    número de ranura
     * @param memento la partida a conservar
     * @param savedAt momento del guardado
     * @throws SaveStorageException si falla el almacenamiento
     */
    void save(int slot, GameMemento memento, Instant savedAt);

    /**
     * Lee la partida guardada en la ranura, comprobando existencia, formato, versión y suma de control en ese orden
     * (RN-33).
     *
     * @param slot número de ranura
     * @throws SaveLoadException    si la ranura está vacía, dañada o es de una versión no admitida
     * @throws SaveStorageException si falla el almacenamiento
     */
    GameMemento load(int slot);

    /**
     * El estado de cada ranura, en orden. Una ranura dañada no detiene el resto de la lista.
     *
     * @throws SaveStorageException si falla el almacenamiento
     */
    List<SlotInfo> slots();
}
