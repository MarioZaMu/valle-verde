package edu.ucc.farmsim.interfaz.pantallas;

import edu.ucc.farmsim.aplicacion.controlador.ComandosJuego;
import edu.ucc.farmsim.aplicacion.dto.VistaAccionEdificio;
import edu.ucc.farmsim.aplicacion.dto.VistaAvatar;
import edu.ucc.farmsim.aplicacion.dto.VistaConversacion;
import edu.ucc.farmsim.aplicacion.dto.VistaEdificio;
import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.dto.VistaMapa;
import edu.ucc.farmsim.aplicacion.dto.VistaNpc;
import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
import edu.ucc.farmsim.aplicacion.dto.VistaRanuraGuardado;
import edu.ucc.farmsim.aplicacion.dto.VistaResumenDia;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.interfaz.componentes.Atajo;
import edu.ucc.farmsim.interfaz.componentes.ColaAviso;
import edu.ucc.farmsim.interfaz.componentes.Herramienta;
import edu.ucc.farmsim.interfaz.componentes.ModeloHerramienta;
import edu.ucc.farmsim.interfaz.componentes.PresentadorAviso;
import edu.ucc.farmsim.interfaz.tema.Textos;
import java.util.function.Consumer;

/**
 * Lógica de la pantalla de juego, sin Swing: qué pasa cuando el jugador hace clic en una parcela, pulsa un botón o usa
 * un atajo. Los mensajes van a un destino (la barra de estado en producción); una cadena vacía borra la barra.
 *
 * <p>Stories: HU-10, HU-17, HU-18, HU-20, HU-22, HU-26, HU-29, HU-30, HU-34, HU-36, HU-39, HU-45, HU-46.
 */
public final class PresentadorPantallaJuego {

    private static final String LIMPIAR = "";

    private final ComandosJuego controlador;
    private final ModeloHerramienta herramientas;
    private final Consumer<String> mensajes;
    private final DialogosJuego dialogos;
    private final PresentadorAviso avisos;
    private int ranuraUltima;
    private boolean bloqueado;

    /**
     * Crea el presentador.
     *
     * @param controlador punto de entrada al juego
     * @param herramientas      la herramienta y la semilla seleccionadas
     * @param mensajes   donde se envían los mensajes para el jugador
     * @param dialogos    abre los diálogos
     * @param avisos     donde se muestran los errores y avisos como toasts (HU-39)
     */
    public PresentadorPantallaJuego(ComandosJuego controlador, ModeloHerramienta herramientas, Consumer<String> mensajes,
                               DialogosJuego dialogos, PresentadorAviso avisos) {
        this.controlador = controlador;
        this.herramientas = herramientas;
        this.mensajes = mensajes;
        this.dialogos = dialogos;
        this.avisos = avisos;
    }

    /**
     * Crea el presentador sin avisos: solo el destino de mensajes se entera del resultado de una orden.
     *
     * @param controlador punto de entrada al juego
     * @param herramientas      la herramienta y la semilla seleccionadas
     * @param mensajes   donde se envían los mensajes para el jugador
     * @param dialogos    abre los diálogos
     */
    public PresentadorPantallaJuego(ComandosJuego controlador, ModeloHerramienta herramientas, Consumer<String> mensajes,
                               DialogosJuego dialogos) {
        this(controlador, herramientas, mensajes, dialogos, new PresentadorAviso(new ColaAviso(() -> 0L)));
    }

    /** Envía un mensaje a la barra de estado; un rechazo también se vuelve un aviso de error (HU-39 · CA-2). */
    private void show(boolean ok, String onExito, String onFallo) {
        mensajes.accept(ok ? onExito : onFallo);
        if (!ok) {
            avisos.error(onFallo);
        } else if (!onExito.isEmpty()) {
            avisos.info(onExito);
        }
    }

    /**
     * «Terminar día»: la barra superior se actualiza por los eventos; solo aparece un mensaje si el día no pudo
     * terminar.
     */
    public void onTerminarDia() {
        Resultado<VistaHud> resultado = controlador.diaFin();
        show(resultado.isOk(), LIMPIAR, resultado.mensaje());
    }

    /**
     * Un clic en una parcela: la herramienta seleccionada actúa sobre ella. Sin herramienta no pasa nada, salvo sobre
     * un edificio, cuyo panel se abre. Construir abre el diálogo de construcción; Alimentar y Cosechar actúan sobre el
     * edificio bajo el clic (HU-36 · CA-4).
     */
    public void onParcelaClicada(int col, int fila) {
        Resultado<VistaMapa> map = controlador.map();
        VistaNpc npc = map.isOk() ? map.value().npcEn(col, fila) : null;
        if (npc != null) {
            onHablar(npc.id());
            return;
        }
        Herramienta herramienta = herramientas.actual();
        if (herramienta == null) {
            VistaEdificio edificio = edificioEn(col, fila);
            if (edificio != null) {
                dialogos.edificioMostrar(edificio.id());
            }
            return;
        }
        if (herramienta == Herramienta.BUILD) {
            dialogos.construccionMostrar(col, fila);
            return;
        }
        if (herramienta == Herramienta.SOW && herramientas.semillaSeleccionada() == null) {
            show(false, LIMPIAR, Textos.NO_SEMILLA_SELECCIONADA);
            return;
        }
        VistaEdificio edificio = herramienta == Herramienta.FEED || herramienta == Herramienta.HARVEST ? edificioEn(col, fila) : null;
        if (herramienta == Herramienta.HARVEST && edificio != null && edificio.capacidad() == 0) {
            edificio = null;
        }
        if (herramienta == Herramienta.FEED) {
            onAlimentar(edificio);
        } else if (herramienta == Herramienta.HARVEST && edificio != null) {
            onRecolectar(edificio);
        } else {
            Resultado<VistaParcela> resultado = switch (herramienta) {
                case SOW -> controlador.plantar(col, fila, herramientas.semillaSeleccionada());
                case WATER -> controlador.agua(col, fila);
                default -> controlador.cosecha(col, fila);
            };
            show(resultado.isOk(), LIMPIAR, resultado.mensaje());
        }
    }

    /**
     * Una flecha o W A S D: un paso del avatar (RN-36). Un paso bloqueado muestra su mensaje; mantener la tecla contra
     * una pared muestra el aviso una vez, no en cada repetición.
     *
     * @param direccion {@code NORTH}, {@code SOUTH}, {@code EAST} or {@code WEST}
     * @return {@code true} si el avatar se movió; {@code false} si solo giró (o no hay partida)
     */
    public boolean onMover(String direccion) {
        Resultado<VistaAvatar> resultado = controlador.mover(direccion);
        if (resultado.isOk()) {
            bloqueado = false;
            mensajes.accept(LIMPIAR);
            return true;
        }
        mensajes.accept(resultado.mensaje());
        if (!bloqueado) {
            avisos.error(resultado.mensaje());
        }
        bloqueado = true;
        return false;
    }

    /**
     * Espacio: la herramienta seleccionada actúa sobre la parcela que tiene enfrente el avatar, con las mismas reglas
     * que un clic allí (RN-36).
     */
    public void onHerramientaUsar() {
        Resultado<VistaMapa> map = controlador.map();
        if (!map.isOk()) {
            show(false, LIMPIAR, map.mensaje());
            return;
        }
        VistaAvatar avatar = map.value().avatar();
        int col = avatar == null ? -1 : avatar.colFrente();
        int fila = avatar == null ? -1 : avatar.filaFrente();
        if (col < 0 || fila < 0 || col >= map.value().cols() || fila >= map.value().filas()) {
            show(false, LIMPIAR, Textos.NADA_EN_FRENTE);
            return;
        }
        onParcelaClicada(col, fila);
    }

    /**
     * E: hablar con la persona que está frente al avatar, o si no con cualquier persona a su lado (RN-37).
     */
    public void onClaveHablar() {
        Resultado<VistaMapa> map = controlador.map();
        if (!map.isOk()) {
            show(false, LIMPIAR, map.mensaje());
            return;
        }
        VistaAvatar avatar = map.value().avatar();
        if (avatar == null) {
            show(false, LIMPIAR, Textos.NADIE_A_HABLAR);
            return;
        }
        VistaNpc npc = map.value().npcEn(avatar.colFrente(), avatar.filaFrente());
        if (npc == null) {
            npc = map.value().npcs().stream().filter(n -> n.esSiguienteA(avatar.col(), avatar.fila())).findFirst()
                    .orElse(null);
        }
        if (npc == null) {
            show(false, LIMPIAR, Textos.NADIE_A_HABLAR);
            return;
        }
        onHablar(npc.id());
    }

    /**
     * Habla con una persona del valle: se abre su diálogo, o se muestra la razón por la que no se puede (muy lejos).
     */
    public void onHablar(String npcId) {
        Resultado<VistaConversacion> resultado = controlador.hablar(npcId);
        if (resultado.isOk()) {
            mensajes.accept(LIMPIAR);
            dialogos.conversacionMostrar(resultado.value());
        } else {
            show(false, LIMPIAR, resultado.mensaje());
        }
    }

    /** Llegó el resumen del día: se muestra al jugador (HU-17). */
    public void onResumen(VistaResumenDia resumen) {
        dialogos.resumenMostrar(resumen);
    }

    private void onAlimentar(VistaEdificio edificio) {
        if (edificio == null) {
            show(false, LIMPIAR, Textos.NO_AQUI_EDIFICIO);
        } else {
            reporte(controlador.alimentar(edificio.id()));
        }
    }

    private void onRecolectar(VistaEdificio edificio) {
        reporte(controlador.collect(edificio.id()));
    }

    private void reporte(Resultado<VistaAccionEdificio> resultado) {
        if (!resultado.isOk()) {
            show(false, LIMPIAR, resultado.mensaje());
        } else {
            String notificacion = resultado.value().notificacion();
            mensajes.accept(notificacion == null ? LIMPIAR : notificacion);
            if (notificacion != null) {
                avisos.info(notificacion);
            }
        }
    }

    private VistaEdificio edificioEn(int col, int fila) {
        Resultado<VistaMapa> map = controlador.map();
        return map.isOk() ? map.value().edificioEn(col, fila) : null;
    }

    /** El botón Guardar: se abren las ranuras para que el jugador elija dónde guardar (CU-14, HU-29). */
    public void onGuardar() {
        dialogos.ranurasMostrar(true);
    }

    /** La última ranura guardada o cargada, o 0 si no se ha usado ninguna. */
    public int ranuraUltima() {
        return ranuraUltima;
    }

    /** Recuerda la última ranura que el jugador guardó o cargó, para las teclas rápidas. */
    public void ranuraUsar(int ranura) {
        this.ranuraUltima = ranura;
    }

    /**
     * Un atajo de teclado (RN-34): terminar el día, abrir el mercado, el inventario o los logros, y guardar o cargar
     * rápido en la última ranura usada (se abre el diálogo de ranuras si todavía no se ha usado ninguna).
     */
    public void onAtajo(Atajo atajo) {
        switch (atajo) {
            case END_DAY -> onTerminarDia();
            case MARKET -> dialogos.mercadoMostrar();
            case INVENTORY -> dialogos.inventarioMostrar();
            case ACHIEVEMENTS -> dialogos.logrosMostrar();
            case QUICK_SAVE -> guardadoRapido();
            case QUICK_LOAD -> cargarRapido();
            default -> throw new IllegalStateException("Unhandled shortcut " + atajo);
        }
    }

    private void guardadoRapido() {
        if (ranuraUltima == 0) {
            dialogos.ranurasMostrar(true);
            return;
        }
        Resultado<VistaRanuraGuardado> resultado = controlador.save(ranuraUltima);
        show(resultado.isOk(), Textos.ARCHIVADO_EN_RANURA + ranuraUltima + ".", resultado.mensaje());
    }

    private void cargarRapido() {
        if (ranuraUltima == 0) {
            dialogos.ranurasMostrar(false);
            return;
        }
        Resultado<VistaHud> resultado = controlador.cargar(ranuraUltima);
        show(resultado.isOk(), Textos.CARGADO_DESDE_RANURA + ranuraUltima + ".", resultado.mensaje());
    }
}
