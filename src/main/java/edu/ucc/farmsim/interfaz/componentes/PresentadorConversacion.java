package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.aplicacion.controlador.ComandosJuego;
import edu.ucc.farmsim.aplicacion.dto.VistaConversacion;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.interfaz.tema.Textos;

/**
 * Lógica del cuadro de diálogo con una persona del valle, sin Swing (HU-46, HU-47): lo que dice, si su encargo todavía
 * se puede entregar, y la entrega en sí con su respuesta o su rechazo.
 *
 * <p>Stories: HU-46, HU-47.
 */
public final class PresentadorConversacion {

    private final ComandosJuego controlador;
    private VistaConversacion vista;
    private String mensaje = "";

    /**
     * Crea el presentador.
     *
     * @param controlador punto de entrada al juego
     * @param first      lo que dijo la persona cuando el jugador empezó a hablar
     */
    public PresentadorConversacion(ComandosJuego controlador, VistaConversacion first) {
        this.controlador = controlador;
        this.vista = first;
    }

    /** Lo que dice ahora la persona. */
    public VistaConversacion vista() {
        return vista;
    }

    /** La respuesta a la última entrega, o un texto vacío. */
    public String mensaje() {
        return mensaje;
    }

    /** Verdadero mientras el encargo no está hecho, de modo que se ofrece el botón Entregar. */
    public boolean puedeEntregar() {
        return !vista.encargoHecho();
    }

    /** Entrega el encargo; si tiene éxito la persona agradece al jugador, y si no se muestra el mensaje de RN-35. */
    public String entregar() {
        Resultado<VistaConversacion> resultado = controlador.entregar(vista.npcId());
        if (resultado.isOk()) {
            vista = resultado.value();
            mensaje = Textos.ENCARGO_ENTREGADO;
        } else {
            mensaje = resultado.mensaje();
        }
        return mensaje;
    }
}
