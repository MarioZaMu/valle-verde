package edu.ucc.farmsim.dominio.progreso;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Guarda los logros que el jugador ha desbloqueado y los contadores que necesitan (RN-28). Un logro se concede una vez
 * y nunca se revoca; evaluarlo solo necesita un {@link HechosProgreso}, nunca la partida.
 *
 * <p>Stories: HU-18, HU-26.
 */
public final class SeguidorLogro {

    private final Set<Logro> desbloqueado = EnumSet.noneOf(Logro.class);
    private int cosechas;
    private int unidadesVendidos;
    private int edificiosConstruidos;
    private int animalesCurados;

    /** Verdadero si el jugador tiene el logro. */
    public boolean has(Logro logro) {
        return desbloqueado.contains(logro);
    }

    /**
     * Concede un logro.
     *
     * @return {@code true} si era nuevo; {@code false} si el jugador ya lo tenía
     */
    public boolean desbloquear(Logro logro) {
        return desbloqueado.add(logro);
    }

    /** Cuenta un cultivo cosechado. */
    public void cosecharRegistro() {
        cosechas++;
    }

    /** Cuenta unidades vendidas. */
    public void ventaRegistro(int unidades) {
        unidadesVendidos += unidades;
    }

    /** Cuenta un edificio construido. */
    public void construirRegistro() {
        edificiosConstruidos++;
    }

    /** Cuenta un animal curado. */
    public void curarRegistro() {
        animalesCurados++;
    }

    /** Cultivos cosechados hasta ahora. */
    public int cosechas() {
        return cosechas;
    }

    /** Unidades vendidas hasta ahora. */
    public int unidadesVendidos() {
        return unidadesVendidos;
    }

    /** Edificios construidos hasta ahora. */
    public int edificiosConstruidos() {
        return edificiosConstruidos;
    }

    /** Animales curados hasta ahora. */
    public int animalesCurados() {
        return animalesCurados;
    }

    /**
     * Desbloquea todos los logros cuya condición cumplen los hechos y que el jugador aún no tiene.
     *
     * @return los desbloqueados ahora, en el orden de RN-28
     */
    public List<Logro> evaluar(HechosProgreso hechos) {
        List<Logro> fresco = new ArrayList<>();
        for (Logro logro : Logro.values()) {
            if (!has(logro) && logro.esCumplidoPor(hechos)) {
                desbloqueado.add(logro);
                fresco.add(logro);
            }
        }
        return fresco;
    }

    /** Una copia inmutable de los logros y los contadores, para el memento. */
    public InstantaneaProgreso instantanea() {
        return new InstantaneaProgreso(desbloqueado, cosechas, unidadesVendidos, edificiosConstruidos, animalesCurados);
    }

    /** Reemplaza todo con una copia tomada antes. */
    public void restaurarDesde(InstantaneaProgreso archivado) {
        desbloqueado.clear();
        desbloqueado.addAll(archivado.desbloqueado());
        cosechas = archivado.cosechas();
        unidadesVendidos = archivado.unidadesVendidos();
        edificiosConstruidos = archivado.edificiosConstruidos();
        animalesCurados = archivado.animalesCurados();
    }
}
