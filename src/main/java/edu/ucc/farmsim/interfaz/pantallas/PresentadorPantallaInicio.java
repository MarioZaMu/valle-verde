package edu.ucc.farmsim.interfaz.pantallas;

import edu.ucc.farmsim.aplicacion.controlador.ComandosJuego;
import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.interfaz.componentes.PresentadorRanuras;
import edu.ucc.farmsim.interfaz.tema.Textos;
import java.util.function.LongSupplier;

/**
 * Lógica de la pantalla de inicio, sin Swing: qué pasa cuando se pulsa cada botón.
 *
 * <p>Stories: HU-28, HU-30, HU-33.
 */
public final class PresentadorPantallaInicio {

    private final ComandosJuego controlador;
    private final NavegadorPantalla navegador;
    private final LongSupplier semillas;

    /**
     * Crea el presentador.
     *
     * @param controlador punto de entrada al juego
     * @param navegador  cambia las pantallas y habla con el jugador
     * @param semillas      provee la semilla de cada partida nueva (el reloj en producción, una constante en las pruebas)
     */
    public PresentadorPantallaInicio(ComandosJuego controlador, NavegadorPantalla navegador, LongSupplier semillas) {
        this.controlador = controlador;
        this.navegador = navegador;
        this.semillas = semillas;
    }

    /** «Nueva partida»: pide confirmación si hay una partida en curso, luego inicia una y muestra el juego. */
    public void onJuegoNuevo() {
        navegador.juegoPreparar();
        if (controlador.tieneJuegoEnProgreso() && !navegador.confirmar(Textos.JUEGO_DESCARTAR_CONFIRMAR)) {
            return;
        }
        Resultado<VistaHud> resultado = controlador.juegoNuevo(semillas.getAsLong());
        if (resultado.isOk()) {
            navegador.juegoMostrar();
        } else {
            navegador.mensajeMostrar(resultado.mensaje());
        }
    }

    /** «Granja de demostración»: igual que «Nueva partida», pero la granja ya trae edificios, animales y cultivos. */
    public void onJuegoDemostracion() {
        navegador.juegoPreparar();
        if (controlador.tieneJuegoEnProgreso() && !navegador.confirmar(Textos.JUEGO_DESCARTAR_CONFIRMAR)) {
            return;
        }
        Resultado<VistaHud> resultado = controlador.juegoDemostracion(semillas.getAsLong());
        if (resultado.isOk()) {
            navegador.juegoMostrar();
        } else {
            navegador.mensajeMostrar(resultado.mensaje());
        }
    }

    /** «Cargar»: avisa al jugador cuando no hay nada que cargar, o le deja elegir una ranura y la carga. */
    public void onCargar() {
        navegador.juegoPreparar();
        Resultado<Boolean> archivado = controlador.tieneJuegosArchivados();
        if (!archivado.isOk()) {
            navegador.mensajeMostrar(archivado.mensaje());
        } else if (!archivado.value()) {
            navegador.mensajeMostrar(Textos.NO_JUEGOS_ARCHIVADOS);
        } else {
            elegirYCargar();
        }
    }

    private void elegirYCargar() {
        PresentadorRanuras ranuras = new PresentadorRanuras(controlador);
        if (!ranuras.error().isEmpty()) {
            navegador.mensajeMostrar(ranuras.error());
            return;
        }
        int ranura = navegador.elegir(Textos.RANURA_ELEGIR, ranuras.lineas());
        if (ranura == 0) {
            return;
        }
        Resultado<VistaHud> resultado = ranuras.cargar(ranura);
        if (resultado.isOk()) {
            navegador.juegoMostrar();
        } else {
            navegador.mensajeMostrar(resultado.mensaje());
        }
    }

    /** «Salir»: cierra la aplicación. */
    public void onSalir() {
        navegador.exit();
    }
}
