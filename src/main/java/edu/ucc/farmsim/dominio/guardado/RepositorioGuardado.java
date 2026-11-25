package edu.ucc.farmsim.dominio.guardado;

import edu.ucc.farmsim.dominio.juego.MementoJuego;
import java.time.Instant;
import java.util.List;

/**
 * Puerto que define el dominio y que implementa la infraestructura: dónde se guardan las partidas (inversión de
 * dependencias, DT-11). Todo fallo técnico del almacenamiento es una {@link ExcepcionAlmacenamientoGuardado}; una ranura que no se
 * puede convertir en partida es una {@link ExcepcionCargarGuardado}.
 *
 * <p>Stories: HU-29, HU-30, HU-31, HU-32, HU-33.
 */
public interface RepositorioGuardado {

    /**
     * Si una ranura contiene una partida guardada.
     *
     * @param ranura número de ranura, de 1 al número de ranuras
     */
    boolean existeRanura(int ranura);

    /**
     * Escribe la partida en la ranura en una sola operación atómica: el contenido anterior se reemplaza o, si falla la
     * escritura, queda como estaba (RN-31).
     *
     * @param ranura    número de ranura
     * @param memento la partida a conservar
     * @param archivadoEn momento del guardado
     * @throws ExcepcionAlmacenamientoGuardado si falla el almacenamiento
     */
    void guardar(int ranura, MementoJuego memento, Instant archivadoEn);

    /**
     * Lee la partida guardada en la ranura, comprobando existencia, formato, versión y suma de control en ese orden
     * (RN-33).
     *
     * @param ranura número de ranura
     * @throws ExcepcionCargarGuardado    si la ranura está vacía, dañada o es de una versión no admitida
     * @throws ExcepcionAlmacenamientoGuardado si falla el almacenamiento
     */
    MementoJuego cargar(int ranura);

    /**
     * El estado de cada ranura, en orden. Una ranura dañada no detiene el resto de la lista.
     *
     * @throws ExcepcionAlmacenamientoGuardado si falla el almacenamiento
     */
    List<InfoRanura> ranuras();
}
