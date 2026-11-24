package edu.ucc.farmsim.application.event;

/**
 * Pattern: Observer — Role: Observer interface (anything that wants to hear about game events).
 *
 * <p>Stories: HU-10.
 */
public interface GameListener {

    /** Lo llama el {@link EventBus} cada vez que se publica un evento. */
    void onEvent(GameEvent event);
}
