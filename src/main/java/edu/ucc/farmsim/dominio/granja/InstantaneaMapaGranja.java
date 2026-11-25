package edu.ucc.farmsim.dominio.granja;

import edu.ucc.farmsim.dominio.cultivo.InstantaneaCultivo;
import edu.ucc.farmsim.dominio.edificio.InstantaneaEdificio;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Copia inmutable de lo que cambia en el mapa: un cultivo por parcela en orden de fila ({@code null} donde la parcela
 * no tiene) y los edificios con sus animales. El terreno nunca cambia, así que no se guarda.
 *
 * <p>Stories: HU-01, HU-18, HU-50.
 *
 * @param cultivos     instantánea del cultivo de cada parcela, o {@code null}
 * @param edificios los edificios en el orden en que se construyeron
 */
public record InstantaneaMapaGranja(List<InstantaneaCultivo> cultivos, List<InstantaneaEdificio> edificios) {

    /** Hace de la lista de edificios una copia no modificable. */
    public InstantaneaMapaGranja {
        edificios = List.copyOf(edificios);
    }

    /**
     * Los mismos cultivos en un mapa más grande: un cultivo de un mapa de {@code desdeCols} de ancho conserva su columna
     * y su fila, y las parcelas nuevas no tienen ninguno (RN-40: una partida del mapa de 16 x 10 pasa a ser la esquina
     * noroeste del valle).
     *
     * @throws IllegalArgumentException si los cultivos no llenan filas completas de {@code desdeCols}, o no caben
     */
    public InstantaneaMapaGranja ensanchado(int desdeCols, int aCols, int aFilas) {
        int desdeFilas = cultivos.size() / desdeCols;
        if (cultivos.size() % desdeCols != 0 || desdeCols > aCols || desdeFilas > aFilas) {
            throw new IllegalArgumentException(cultivos.size() + " crops are not a map of " + desdeCols + " columns");
        }
        List<InstantaneaCultivo> amplio = new ArrayList<>(Collections.nCopies(aCols * aFilas, (InstantaneaCultivo) null));
        for (int i = 0; i < cultivos.size(); i++) {
            amplio.set(i / desdeCols * aCols + i % desdeCols, cultivos.get(i));
        }
        return new InstantaneaMapaGranja(Collections.unmodifiableList(amplio), edificios);
    }
}
