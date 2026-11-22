package edu.ucc.farmsim.ui.components;

/**
 * One notice as it must be drawn right now. Immutable.
 *
 * <p>Stories: HU-39.
 *
 * @param text    what it says, in Spanish
 * @param kind    its kind
 * @param opacity 1 while fully visible, falling to 0 while it fades
 */
public record ToastView(String text, ToastKind kind, float opacity) { }
