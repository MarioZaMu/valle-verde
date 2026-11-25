package edu.ucc.farmsim.interfaz.pantallas;

import edu.ucc.farmsim.aplicacion.controlador.ComandosJuego;
import edu.ucc.farmsim.aplicacion.dto.VistaConversacion;
import edu.ucc.farmsim.aplicacion.dto.VistaResumenDia;
import edu.ucc.farmsim.aplicacion.evento.BusEvento;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.interfaz.componentes.Atajo;
import edu.ucc.farmsim.interfaz.componentes.BarraHerramienta;
import edu.ucc.farmsim.interfaz.componentes.BotonMadera;
import edu.ucc.farmsim.interfaz.componentes.CajaMadera;
import edu.ucc.farmsim.interfaz.componentes.CajaTutorial;
import edu.ucc.farmsim.interfaz.componentes.CapaAviso;
import edu.ucc.farmsim.interfaz.componentes.ColaAviso;
import edu.ucc.farmsim.interfaz.componentes.DialogoConstruccion;
import edu.ucc.farmsim.interfaz.componentes.DialogoConversacion;
import edu.ucc.farmsim.interfaz.componentes.DialogoEdificio;
import edu.ucc.farmsim.interfaz.componentes.DialogoInventario;
import edu.ucc.farmsim.interfaz.componentes.DialogoLogros;
import edu.ucc.farmsim.interfaz.componentes.DialogoMercado;
import edu.ucc.farmsim.interfaz.componentes.DialogoRanuras;
import edu.ucc.farmsim.interfaz.componentes.DialogoResumenDia;
import edu.ucc.farmsim.interfaz.componentes.Herramienta;
import edu.ucc.farmsim.interfaz.componentes.HudPanel;
import edu.ucc.farmsim.interfaz.componentes.ModeloHerramienta;
import edu.ucc.farmsim.interfaz.componentes.ModeloInventario;
import edu.ucc.farmsim.interfaz.componentes.PanelSuperposicion;
import edu.ucc.farmsim.interfaz.componentes.PresentadorAviso;
import edu.ucc.farmsim.interfaz.componentes.PresentadorConstruccion;
import edu.ucc.farmsim.interfaz.componentes.PresentadorConversacion;
import edu.ucc.farmsim.interfaz.componentes.PresentadorEdificio;
import edu.ucc.farmsim.interfaz.componentes.PresentadorLogros;
import edu.ucc.farmsim.interfaz.componentes.PresentadorMercado;
import edu.ucc.farmsim.interfaz.componentes.PresentadorRanuras;
import edu.ucc.farmsim.interfaz.componentes.PresentadorTutorial;
import edu.ucc.farmsim.interfaz.componentes.TeclasCaminar;
import edu.ucc.farmsim.interfaz.dibujo.EfectosVisuales;
import edu.ucc.farmsim.interfaz.dibujo.PanelMapa;
import edu.ucc.farmsim.interfaz.tema.EstiloMadera;
import edu.ucc.farmsim.interfaz.tema.Textos;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * La pantalla principal del juego: la granja llena la ventana y las barras flotan sobre ella en cajas de madera, la
 * fecha, el clima, el dinero y la energía en la esquina superior derecha (HUD), y abajo la barra de herramientas, una
 * línea de estado para mensajes y los botones «Terminar día» y «Menú». Las teclas 1 a 5 eligen una herramienta, Escape
 * la deselecciona, y los atajos de RN-34 funcionan en cualquier parte de la pantalla. Mientras corre el tutorial de
 * inicio, el cuadro del avatar flota en la esquina superior izquierda (HU-49).
 *
 * <p>Stories: HU-17, HU-18, HU-20, HU-22, HU-23, HU-26, HU-29, HU-30, HU-33, HU-34, HU-35, HU-36, HU-37, HU-38, HU-39, HU-40, HU-41, HU-45, HU-46, HU-48, HU-49, HU-50.
 */
public final class PantallaJuego extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String DESELECCIONAR = "deselect-tool";
    private static final String HERRAMIENTA_USAR = "use-tool";
    private static final String TALK = "hablar";
    private static final int HUECO = 6;
    private static final int MARGEN = 8;
    private static final int MS_TIC_CAMINAR = 15;

    /**
     * Construye la pantalla.
     *
     * @param controlador punto de entrada al juego
     * @param bus        donde se publican los eventos del juego (la barra superior y el mapa lo escuchan)
     * @param navegador  cambia las pantallas
     */
    public PantallaJuego(ComandosJuego controlador, BusEvento bus, NavegadorPantalla navegador) {
        super(new BorderLayout());
        ModeloHerramienta herramientas = new ModeloHerramienta();
        JLabel status = new JLabel(" ");
        ModeloInventario inventario = new ModeloInventario(controlador, herramientas);
        bus.suscribir(inventario);
        inventario.refrescar();
        PresentadorPantallaJuego[] refPresentador = new PresentadorPantallaJuego[1];
        DialogosJuego dialogos = new DialogosJuego() {
            @Override
            public void mercadoMostrar() {
                new DialogoMercado(SwingUtilities.getWindowAncestor(PantallaJuego.this), new PresentadorMercado(controlador))
                        .setVisible(true);
            }

            @Override
            public void inventarioMostrar() {
                new DialogoInventario(SwingUtilities.getWindowAncestor(PantallaJuego.this), inventario).setVisible(true);
            }

            @Override
            public void construccionMostrar(int col, int fila) {
                new DialogoConstruccion(SwingUtilities.getWindowAncestor(PantallaJuego.this),
                        new PresentadorConstruccion(controlador, col, fila)).setVisible(true);
            }

            @Override
            public void edificioMostrar(int idEdificio) {
                new DialogoEdificio(SwingUtilities.getWindowAncestor(PantallaJuego.this),
                        new PresentadorEdificio(controlador, idEdificio)).setVisible(true);
            }

            @Override
            public void logrosMostrar() {
                new DialogoLogros(SwingUtilities.getWindowAncestor(PantallaJuego.this),
                        new PresentadorLogros(controlador)).setVisible(true);
            }

            @Override
            public void ranurasMostrar(boolean guardando) {
                new DialogoRanuras(SwingUtilities.getWindowAncestor(PantallaJuego.this), new PresentadorRanuras(controlador),
                        ranura -> refPresentador[0].ranuraUsar(ranura), () -> status.setText(Textos.CARGADO_DESDE_RANURA
                        + refPresentador[0].ranuraUltima() + ".")).setVisible(true);
            }

            @Override
            public void resumenMostrar(VistaResumenDia resumen) {
                new DialogoResumenDia(SwingUtilities.getWindowAncestor(PantallaJuego.this), resumen).setVisible(true);
            }

            @Override
            public void conversacionMostrar(VistaConversacion conversacion) {
                new DialogoConversacion(SwingUtilities.getWindowAncestor(PantallaJuego.this),
                        new PresentadorConversacion(controlador, conversacion)).setVisible(true);
            }
        };
        ColaAviso colaAviso = new ColaAviso(System::currentTimeMillis);
        PresentadorAviso avisos = new PresentadorAviso(colaAviso);
        bus.suscribir(avisos);
        PresentadorPantallaJuego presentador = new PresentadorPantallaJuego(controlador, herramientas, status::setText, dialogos, avisos);
        refPresentador[0] = presentador;
        bus.suscribir(evento -> {
            if (evento instanceof EventoJuego.ResumenDiaListo listo) {
                presentador.onResumen(listo.resumen());
            } else if (evento instanceof EventoJuego.LogroDesbloqueado desbloqueado) {
                status.setText(Textos.logroDesbloqueado(desbloqueado.name(), desbloqueado.recompensa()));
            }
        });

        PanelMapa map = new PanelMapa(controlador, bus, presentador::onParcelaClicada);
        HudPanel hud = new HudPanel(bus);
        CajaTutorial tutorial = new CajaTutorial(new PresentadorTutorial(controlador));
        bus.suscribir(tutorial);
        tutorial.refrescar();
        JPanel filaTutorial = clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)));
        filaTutorial.add(tutorial);
        JPanel arriba = clear(new JPanel(new BorderLayout(HUECO, 0)));
        arriba.add(filaTutorial, BorderLayout.WEST);
        arriba.add(hud, BorderLayout.CENTER);

        CajaMadera cajaHerramienta = new CajaMadera(new BorderLayout());
        cajaHerramienta.add(new BarraHerramienta(herramientas), BorderLayout.CENTER);
        JPanel filaHerramienta = clear(new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0)));
        filaHerramienta.add(cajaHerramienta);

        status.setForeground(EstiloMadera.TINTA);
        status.setFont(status.getFont().deriveFont(Font.BOLD));
        CajaMadera cajaSituacion = new CajaMadera(new BorderLayout());
        cajaSituacion.add(status, BorderLayout.CENTER);
        JButton diaFin = new BotonMadera(Textos.END_DAY);
        diaFin.addActionListener(evento -> presentador.onTerminarDia());
        JButton save = new BotonMadera(Textos.GUARDADO);
        save.setToolTipText(Textos.PISTA_GUARDADO);
        save.addActionListener(evento -> presentador.onGuardar());
        JButton menu = new BotonMadera(Textos.MENU);
        menu.addActionListener(evento -> navegador.inicioMostrar());
        JCheckBox efectos = new JCheckBox(Textos.EFECTOS, true);
        efectos.setFocusable(false);
        efectos.setOpaque(false);
        efectos.setForeground(EstiloMadera.TINTA);
        efectos.addActionListener(evento -> map.efectosFijarHabilitados(efectos.isSelected()));
        CajaMadera cajaEfectos = new CajaMadera(new BorderLayout());
        cajaEfectos.add(efectos, BorderLayout.CENTER);
        JPanel botones = clear(new JPanel(new FlowLayout(FlowLayout.RIGHT, HUECO, 0)));
        botones.add(diaFin);
        botones.add(save);
        botones.add(menu);
        botones.add(cajaEfectos);
        JPanel filaMensaje = clear(new JPanel(new BorderLayout(HUECO, 0)));
        filaMensaje.add(cajaSituacion, BorderLayout.CENTER);
        filaMensaje.add(botones, BorderLayout.EAST);

        JPanel inferior = clear(new JPanel());
        inferior.setLayout(new BoxLayout(inferior, BoxLayout.Y_AXIS));
        inferior.add(filaHerramienta);
        inferior.add(Box.createVerticalStrut(HUECO));
        inferior.add(filaMensaje);

        JPanel barras = clear(new JPanel(new BorderLayout()));
        barras.setBorder(BorderFactory.createEmptyBorder(MARGEN, MARGEN, MARGEN, MARGEN));
        barras.add(arriba, BorderLayout.NORTH);
        barras.add(inferior, BorderLayout.SOUTH);
        map.agregarFlotante(hud.caja());
        map.agregarFlotante(tutorial);
        map.agregarFlotante(inferior);

        PanelSuperposicion etapa = new PanelSuperposicion(map);
        etapa.agregarOnArriba(barras);
        etapa.agregarOnArriba(new CapaAviso(colaAviso));
        add(etapa, BorderLayout.CENTER);

        clavesEnlazar(herramientas, presentador);
        caminandoEnlazar(presentador, map);
    }

    private static long now() {
        return System.nanoTime() / 1_000_000L;
    }

    /**
     * Un paso. Cuando está bloqueado y otra tecla pulsada apunta en ángulo recto (caminando en diagonal a lo largo de
     * una cerca de árboles), se prueba ese camino en su lugar; un paso que sigue bloqueado solo gira al avatar, lo que
     * ningún evento anuncia, así que el mapa que se muestra lo gira sin pedirle al servidor todo el valle.
     */
    private static void paso(PresentadorPantallaJuego presentador, PanelMapa map, TeclasCaminar caminando, String direccion) {
        if (presentador.onMover(direccion)) {
            return;
        }
        String enLugar = caminando.alternativa(direccion);
        if (enLugar != null && presentador.onMover(enLugar)) {
            caminando.enLugarPisado(enLugar);
            return;
        }
        map.cara(direccion);
    }

    private static JPanel clear(JPanel panel) {
        panel.setOpaque(false);
        return panel;
    }

    /**
     * Las flechas y W A S D hacen caminar al avatar: una pulsación da un paso de inmediato y las teclas mantenidas
     * siguen caminando, una parcela cada {@link EfectosVisuales#MS_PASO}, sin pausa después del primer paso; dos teclas
     * perpendiculares (W y D) caminan en diagonal (RN-40). Espacio usa la herramienta seleccionada en la parcela de
     * enfrente (HU-45) y E habla con la persona junto al avatar (HU-46).
     */
    private void caminandoEnlazar(PresentadorPantallaJuego presentador, PanelMapa map) {
        InputMap claves = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        TeclasCaminar caminando = new TeclasCaminar(EfectosVisuales.MS_PASO);
        Timer ritmo = new Timer(MS_TIC_CAMINAR, null);
        ritmo.addActionListener(evento -> {
            Window ventana = SwingUtilities.getWindowAncestor(this);
            if (ventana == null || !ventana.isFocused()) {
                caminando.clear();
            }
            String direccion = caminando.tic(now());
            if (direccion != null) {
                paso(presentador, map, caminando, direccion);
            }
            if (!caminando.esCaminando()) {
                ritmo.stop();
            }
        });
        Object[][] camina = {
            {"NORTH", KeyEvent.VK_UP, KeyEvent.VK_W}, {"SOUTH", KeyEvent.VK_DOWN, KeyEvent.VK_S},
            {"EAST", KeyEvent.VK_RIGHT, KeyEvent.VK_D}, {"WEST", KeyEvent.VK_LEFT, KeyEvent.VK_A},
        };
        for (Object[] walk : camina) {
            String direccion = (String) walk[0];
            String abajo = "walk-" + direccion;
            String up = "stop-" + direccion;
            for (int i = 1; i <= 2; i++) {
                claves.put(KeyStroke.getKeyStroke((Integer) walk[i], 0, false), abajo);
                claves.put(KeyStroke.getKeyStroke((Integer) walk[i], 0, true), up);
            }
            getActionMap().put(abajo, new AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    String first = caminando.pulsar(direccion, now());
                    if (first != null) {
                        paso(presentador, map, caminando, first);
                        ritmo.start();
                    }
                }
            });
            getActionMap().put(up, new AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    caminando.liberar(direccion);
                }
            });
        }
        claves.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), TALK);
        getActionMap().put(TALK, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                presentador.onClaveHablar();
            }
        });
        claves.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), HERRAMIENTA_USAR);
        getActionMap().put(HERRAMIENTA_USAR, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                presentador.onHerramientaUsar();
            }
        });
    }

    private void clavesEnlazar(ModeloHerramienta herramientas, PresentadorPantallaJuego presentador) {
        InputMap claves = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        for (Herramienta herramienta : Herramienta.values()) {
            String name = "tool-" + herramienta.name();
            claves.put(KeyStroke.getKeyStroke(herramienta.codigoClave(), 0), name);
            getActionMap().put(name, new AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    herramientas.seleccionar(herramienta);
                }
            });
        }
        claves.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), DESELECCIONAR);
        getActionMap().put(DESELECCIONAR, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                herramientas.deseleccionar();
            }
        });
        for (Atajo atajo : Atajo.values()) {
            String name = "shortcut-" + atajo.name();
            claves.put(KeyStroke.getKeyStroke(atajo.codigoClave(), 0), name);
            getActionMap().put(name, new AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    presentador.onAtajo(atajo);
                }
            });
        }
    }
}
