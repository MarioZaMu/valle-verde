package edu.ucc.farmsim.domain.farm;

import edu.ucc.farmsim.domain.building.BuildingSnapshot;
import edu.ucc.farmsim.domain.crop.CropSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Copia inmutable de lo que cambia en el mapa: un cultivo por parcela en orden de fila ({@code null} donde la parcela
 * no tiene) y los edificios con sus animales. El terreno nunca cambia, así que no se guarda.
 *
 * <p>Stories: HU-01, HU-18, HU-50.
 *
 * @param crops     instantánea del cultivo de cada parcela, o {@code null}
 * @param buildings los edificios en el orden en que se construyeron
 */
public record FarmMapSnapshot(List<CropSnapshot> crops, List<BuildingSnapshot> buildings) {

    /** Hace de la lista de edificios una copia no modificable. */
    public FarmMapSnapshot {
        buildings = List.copyOf(buildings);
    }

    /**
     * Los mismos cultivos en un mapa más grande: un cultivo de un mapa de {@code fromCols} de ancho conserva su columna
     * y su fila, y las parcelas nuevas no tienen ninguno (RN-40: una partida del mapa de 16 x 10 pasa a ser la esquina
     * noroeste del valle).
     *
     * @throws IllegalArgumentException si los cultivos no llenan filas completas de {@code fromCols}, o no caben
     */
    public FarmMapSnapshot widened(int fromCols, int toCols, int toRows) {
        int fromRows = crops.size() / fromCols;
        if (crops.size() % fromCols != 0 || fromCols > toCols || fromRows > toRows) {
            throw new IllegalArgumentException(crops.size() + " crops are not a map of " + fromCols + " columns");
        }
        List<CropSnapshot> wide = new ArrayList<>(Collections.nCopies(toCols * toRows, (CropSnapshot) null));
        for (int i = 0; i < crops.size(); i++) {
            wide.set(i / fromCols * toCols + i % fromCols, crops.get(i));
        }
        return new FarmMapSnapshot(Collections.unmodifiableList(wide), buildings);
    }
}
