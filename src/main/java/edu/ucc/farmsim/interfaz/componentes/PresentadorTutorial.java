package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.aplicacion.controlador.ComandosJuego;
import edu.ucc.farmsim.aplicacion.dto.VistaTutorial;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.interfaz.tema.Textos;

/**
 * La lógica del cuadro del avatar del tutorial de inicio (HU-49), sin Swing: qué mostrar y saltarlo.
 *
 * <p>Stories: HU-49.
 */
public final class PresentadorTutorial {

    private final ComandosJuego controlador;
    private VistaTutorial vista;

    /**
     * Crea el presentador.
     *
     * @param controlador punto de entrada al juego
     */
    public PresentadorTutorial(ComandosJuego controlador) {
        this.controlador = controlador;
    }

    /** Le pide otra vez el tutorial a la partida; sin partida no hay nada que mostrar. */
    public void recargar() {
        Resultado<VistaTutorial> resultado = controlador.tutorial();
        vista = resultado.isOk() ? resultado.value() : null;
    }

    /** Muestra un tutorial que acaba de cambiar, tal como llegó en su evento. */
    public void show(VistaTutorial cambiado) {
        vista = cambiado;
    }

    /** Verdadero mientras hay un tutorial todavía en marcha. */
    public boolean esActivo() {
        return vista != null && !vista.concluido();
    }

    /** «Paso n de 5»; vacío cuando no se muestra nada. */
    public String textoPaso() {
        return esActivo() ? Textos.pasoTutorial(vista.numero(), vista.total()) : "";
    }

    /** Lo que dice ahora el avatar; vacío cuando no se muestra nada. */
    public String textoMensaje() {
        return esActivo() ? vista.texto() : "";
    }

    /** Termina el tutorial. */
    public void saltar() {
        Resultado<VistaTutorial> resultado = controlador.tutorialSaltar();
        if (resultado.isOk()) {
            vista = resultado.value();
        }
    }
}
