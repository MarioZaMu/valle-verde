package edu.ucc.farmsim.aplicacion.dto;

import java.util.List;

/**
 * Todo el mapa de la granja tal como lo dibuja la pantalla: su tamaño y un {@link VistaParcela} por parcela en orden de
 * fila. Inmutable.
 *
 * <p>Stories: HU-35, HU-45, HU-46, HU-50.
 *
 * @param cols      número de columnas
 * @param filas      número de filas
 * @param parcelas     todas las parcelas, fila por fila
 * @param edificios los edificios dibujados sobre las parcelas, con sus animales
 * @param avatar    dónde está el avatar y hacia dónde mira, o {@code null} si no se dibuja
 * @param npcs      las personas del valle, cada una en su parcela
 * @param casas    las casas del valle, dibujadas sobre sus parcelas
 */
public record VistaMapa(int cols, int filas, List<VistaParcela> parcelas, List<VistaEdificio> edificios, VistaAvatar avatar,
                      List<VistaNpc> npcs, List<VistaCasa> casas) {

    /** Un mapa con el avatar y las personas, sin casas. */
    public VistaMapa(int cols, int filas, List<VistaParcela> parcelas, List<VistaEdificio> edificios, VistaAvatar avatar,
                   List<VistaNpc> npcs) {
        this(cols, filas, parcelas, edificios, avatar, npcs, List.of());
    }

    /** Un mapa con el avatar y sin personas. */
    public VistaMapa(int cols, int filas, List<VistaParcela> parcelas, List<VistaEdificio> edificios, VistaAvatar avatar) {
        this(cols, filas, parcelas, edificios, avatar, List.of());
    }

    /** Un mapa sin edificios y sin avatar. */
    public VistaMapa(int cols, int filas, List<VistaParcela> parcelas) {
        this(cols, filas, parcelas, List.of());
    }

    /** Un mapa sin avatar. */
    public VistaMapa(int cols, int filas, List<VistaParcela> parcelas, List<VistaEdificio> edificios) {
        this(cols, filas, parcelas, edificios, null);
    }

    /** El mismo mapa con el avatar en otro lugar o mirando a otro lado (un paso no cambia nada más). */
    public VistaMapa conAvatar(VistaAvatar movido) {
        return new VistaMapa(cols, filas, parcelas, edificios, movido, npcs, casas);
    }

    /** El edificio que cubre la parcela, o {@code null}. */
    public VistaEdificio edificioEn(int col, int fila) {
        for (VistaEdificio edificio : edificios) {
            if (edificio.cubre(col, fila)) {
                return edificio;
            }
        }
        return null;
    }

    /** La persona que está sobre la parcela, o {@code null}. */
    public VistaNpc npcEn(int col, int fila) {
        for (VistaNpc npc : npcs == null ? List.<VistaNpc>of() : npcs) {
            if (npc.col() == col && npc.fila() == fila) {
                return npc;
            }
        }
        return null;
    }

    /**
     * La parcela en las coordenadas dadas.
     *
     * @throws IllegalArgumentException si las coordenadas están fuera del mapa
     */
    public VistaParcela parcelaEn(int col, int fila) {
        if (col < 0 || col >= cols || fila < 0 || fila >= filas) {
            throw new IllegalArgumentException("outside the map: (" + col + ", " + fila + ")");
        }
        return parcelas.get(fila * cols + col);
    }
}
