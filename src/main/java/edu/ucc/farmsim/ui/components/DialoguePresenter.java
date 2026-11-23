package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.dto.DialogueView;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.ui.theme.Texts;

/**
 * Logic of the dialogue box with a person of the valley, with no Swing (HU-46, HU-47): what it says, whether
 * its errand can still be delivered, and the delivery itself with its answer or its rejection.
 *
 * <p>Stories: HU-46, HU-47.
 */
public final class DialoguePresenter {

    private final GameCommands controller;
    private DialogueView view;
    private String message = "";

    /**
     * Creates the presenter.
     *
     * @param controller entry point to the game
     * @param first      what the person said when the player started talking
     */
    public DialoguePresenter(GameCommands controller, DialogueView first) {
        this.controller = controller;
        this.view = first;
    }

    /** What the person says now. */
    public DialogueView view() {
        return view;
    }

    /** The answer to the last delivery, or an empty text. */
    public String message() {
        return message;
    }

    /** True while the errand is not done, so the Deliver button is offered. */
    public boolean canDeliver() {
        return !view.errandDone();
    }

    /** Delivers the errand; on success the person thanks the player, otherwise the RN-35 message is shown. */
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
