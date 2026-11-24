package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.dto.DialogueView;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.ui.theme.Texts;

/**
 * Lógica del cuadro de diálogo con una persona del valle, sin Swing (HU-46, HU-47): lo que dice, si su encargo todavía
 * se puede entregar, y la entrega en sí con su respuesta o su rechazo.
 *
 * <p>Stories: HU-46, HU-47.
 */
public final class DialoguePresenter {

    private final GameCommands controller;
    private DialogueView view;
    private String message = "";

    /**
     * Crea el presentador.
     *
     * @param controller punto de entrada al juego
     * @param first      lo que dijo la persona cuando el jugador empezó a hablar
     */
    public DialoguePresenter(GameCommands controller, DialogueView first) {
        this.controller = controller;
        this.view = first;
    }

    /** Lo que dice ahora la persona. */
    public DialogueView view() {
        return view;
    }

    /** La respuesta a la última entrega, o un texto vacío. */
    public String message() {
        return message;
    }

    /** Verdadero mientras el encargo no está hecho, de modo que se ofrece el botón Entregar. */
    public boolean canDeliver() {
        return !view.errandDone();
    }

    /** Entrega el encargo; si tiene éxito la persona agradece al jugador, y si no se muestra el mensaje de RN-35. */
    public String deliver() {
        Result<DialogueView> result = controller.deliver(view.npcId());
        if (result.isOk()) {
            view = result.value();
            message = Texts.ERRAND_DELIVERED;
        } else {
            message = result.message();
        }
        return message;
    }
}
