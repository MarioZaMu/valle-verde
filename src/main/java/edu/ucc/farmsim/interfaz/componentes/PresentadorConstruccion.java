package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.aplicacion.controlador.ComandosJuego;
import edu.ucc.farmsim.aplicacion.dto.VistaEdificio;
import edu.ucc.farmsim.aplicacion.dto.VistaEdificios;
import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.dto.VistaOfertaEdificio;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Mensajes;
import edu.ucc.farmsim.aplicacion.resultado.Nombres;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.interfaz.tema.Textos;
import java.util.List;
import java.util.Optional;

/**
 * La lógica del diálogo de construcción, sin nada de Swing (HU-18): el catálogo, por qué no se puede construir un
 * edificio y el mensaje después de intentarlo. La esquina del edificio es la parcela en la que se hizo clic.
 *
 * <p>Stories: HU-18.
 */
public final class PresentadorConstruccion {

    private final ComandosJuego juego;
    private final int col;
    private final int fila;
    private List<VistaOfertaEdificio> catalogo = List.of();
    private int dinero;
    private Optional<VistaEdificio> ultimoConstruido = Optional.empty();

    /**
     * Crea el presentador y carga el catálogo.
     *
     * @param juego la partida
     * @param col  columna de la parcela en que se hizo clic
     * @param fila  fila de la parcela en que se hizo clic
     */
    public PresentadorConstruccion(ComandosJuego juego, int col, int fila) {
        this.juego = juego;
        this.col = col;
        this.fila = fila;
        refrescar();
    }

    /** Carga otra vez el catálogo y el saldo. */
    public void refrescar() {
        Resultado<VistaEdificios> edificios = juego.edificios();
        if (edificios.isOk()) {
            catalogo = edificios.value().catalogo();
        }
        Resultado<VistaHud> hud = juego.hud();
        if (hud.isOk()) {
            dinero = hud.value().dinero();
        }
    }

    /** Lo que se puede construir. */
    public List<VistaOfertaEdificio> catalogo() {
        return catalogo;
    }

    /** El texto bajo el título, por ejemplo {@code Esquina en (2, 7)}. */
    public String textoColocar() {
        return "Esquina superior izquierda en (" + col + ", " + fila + ")";
    }

    /** El dinero que tiene ahora el jugador, como texto. */
    public String textoDinero() {
        return Textos.dinero(dinero);
    }

    /** La fila de una oferta tal como la muestra la tabla. */
    public Object[] fila(VistaOfertaEdificio oferta) {
        return new Object[] {oferta.name(), Textos.size(oferta.width(), oferta.height()), Textos.dinero(oferta.costo()),
                estado(oferta)};
    }

    /** Lo que dice la columna de estado: bloqueado (y por qué), el límite alcanzado, o cuántos hay construidos. */
    public String estado(VistaOfertaEdificio oferta) {
        if (!oferta.desbloqueado()) {
            return "Bloqueado: requiere el logro «" + oferta.requisito() + "»";
        }
        return oferta.construido() + "/" + oferta.max() + " construidos";
    }

    /**
     * La razón por la que ahora no se puede construir el edificio, con las palabras de RN-35, o un texto vacío si se
     * puede.
     */
    public String bloquear(VistaOfertaEdificio oferta) {
        if (!oferta.desbloqueado()) {
            return Mensajes.of(CodigoError.EDIFICIO_BLOQUEADO, oferta.requisito());
        }
        if (oferta.construido() >= oferta.max()) {
            return Mensajes.of(CodigoError.LIMITE_EDIFICIOS, Nombres.edificioPlural(oferta.tipo()));
        }
        if (oferta.costo() > dinero) {
            return Mensajes.of(CodigoError.SIN_FONDOS, oferta.costo() - dinero);
        }
        return "";
    }

    /** Si el botón de construir está habilitado para la oferta. */
    public boolean puedeConstruir(VistaOfertaEdificio oferta) {
        return bloquear(oferta).isEmpty();
    }

    /**
     * Construye en la parcela donde se hizo clic.
     *
     * @return el mensaje que se muestra; el resultado está disponible mediante {@link #ultimoConstruido()}
     */
    public String build(VistaOfertaEdificio oferta) {
        Resultado<VistaEdificio> resultado = juego.build(oferta.tipo(), col, fila);
        refrescar();
        ultimoConstruido = resultado.isOk() ? Optional.of(resultado.value()) : Optional.empty();
        return resultado.isOk() ? "Construiste: " + oferta.name() + "." : resultado.mensaje();
    }

    /** El edificio que creó el último {@link #build} exitoso. */
    public Optional<VistaEdificio> ultimoConstruido() {
        return ultimoConstruido;
    }
}
