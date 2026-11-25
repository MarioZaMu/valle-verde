package edu.ucc.farmsim.dominio.granja;

import edu.ucc.farmsim.dominio.cultivo.Cultivo;
import edu.ucc.farmsim.dominio.edificio.Edificio;
import edu.ucc.farmsim.dominio.edificio.TipoEdificio;

/**
 * Un cuadro del mapa de la granja: su terreno, el cultivo que crece en él (sobre tierra) o el edificio que lo cubre
 * (sobre pasto).
 *
 * <p>Stories: HU-01, HU-25, HU-28.
 */
public final class Parcela {

    private final Terreno terreno;
    private Cultivo cultivo;
    private Edificio edificio;

    /**
     * Crea una parcela.
     *
     * @param terreno tipo de suelo
     */
    public Parcela(Terreno terreno) {
        this.terreno = terreno;
    }

    /** Tipo de suelo. */
    public Terreno terreno() {
        return terreno;
    }

    /** El cultivo que crece aquí, o {@code null}. */
    public Cultivo cultivo() {
        return cultivo;
    }

    /** Verdadero si un cultivo (vivo o marchito) ocupa la parcela. */
    public boolean tieneCultivo() {
        return cultivo != null;
    }

    /**
     * Planta un cultivo.
     *
     * @throws IllegalStateException si la parcela no es tierra o ya tiene un cultivo
     */
    public void plantar(Cultivo cultivoNuevo) {
        if (!puedeSerSembrado()) {
            throw new IllegalStateException("Only soil or a greenhouse plot can be sown: " + terreno);
        }
        if (cultivo != null) {
            throw new IllegalStateException("The plot already has a crop");
        }
        this.cultivo = cultivoNuevo;
    }

    /** Verdadero si un invernadero cubre la parcela: su cultivo es un cultivo protegido (RN-22). */
    public boolean esResguardado() {
        return edificio != null && edificio.tipo() == TipoEdificio.GREENHOUSE;
    }

    /** Verdadero si aquí se puede sembrar: tierra, o una parcela de un invernadero (RN-06, RN-22). */
    public boolean puedeSerSembrado() {
        return terreno == Terreno.SOIL || esResguardado();
    }

    /** Quita el cultivo (cosecha o limpieza). */
    public void cultivoQuitar() {
        this.cultivo = null;
    }

    /** El edificio que cubre esta parcela, o {@code null}. */
    public Edificio edificio() {
        return edificio;
    }

    void edificioFijar(Edificio cubriendo) {
        this.edificio = cubriendo;
    }

    void cultivoRestaurar(Cultivo restaurado) {
        this.cultivo = restaurado;
    }
}
