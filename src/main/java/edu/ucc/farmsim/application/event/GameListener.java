package edu.ucc.farmsim.application.event;

/**
 * Pattern: Observer — Role: Observer interface (anything that wants to hear about game events).
 *
 * <p>Stories: HU-10.
 */
public interface GameListener {

    /** Called by the {@link EventBus} each time an event is published. */
    void onEvent(GameEvent event);
}
