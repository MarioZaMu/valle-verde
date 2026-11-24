package edu.ucc.farmsim.ui.components;

/**
 * Un aviso tal como debe dibujarse ahora mismo. Inmutable.
 *
 * <p>Stories: HU-39.
 *
 * @param text    lo que dice, en español
 * @param kind    su tipo
 * @param opacity 1 mientras es totalmente visible, bajando a 0 mientras se desvanece
 */
public record ToastView(String text, ToastKind kind, float opacity) { }
