package edu.ucc.farmsim.interfaz.componentes;

/**
 * Un aviso tal como debe dibujarse ahora mismo. Inmutable.
 *
 * <p>Stories: HU-39.
 *
 * @param texto    lo que dice, en español
 * @param categoria    su tipo
 * @param opacidad 1 mientras es totalmente visible, bajando a 0 mientras se desvanece
 */
public record VistaAviso(String texto, CategoriaAviso categoria, float opacidad) { }
