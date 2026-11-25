package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.aplicacion.controlador.ComandosJuego;
import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.dto.VistaRanuraGuardado;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.interfaz.tema.Textos;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * La lógica de las ranuras de guardado, sin nada de Swing (HU-29, HU-30, HU-32): el texto de cada ranura, guardar con
 * la confirmación que pide sobrescribir, y cargar. Los diálogos solo la dibujan.
 *
 * <p>Stories: HU-29, HU-30, HU-32.
 */
public final class PresentadorRanuras {

    private final ComandosJuego juego;
    private List<VistaRanuraGuardado> ranuras = List.of();
    private String error = "";

    /**
     * Crea el presentador y lee las ranuras.
     *
     * @param juego la partida a la que pertenecen las ranuras
     */
    public PresentadorRanuras(ComandosJuego juego) {
        this.juego = juego;
        refrescar();
    }

    /** Lee las ranuras otra vez (para que un guardado nuevo se vea de inmediato). */
    public void refrescar() {
        Resultado<List<VistaRanuraGuardado>> resultado = juego.guardarRanuras();
        error = resultado.isOk() ? "" : resultado.mensaje();
        if (resultado.isOk()) {
            ranuras = resultado.value();
        }
    }

    /** La razón por la que no se pudieron leer las ranuras, o un texto vacío. */
    public String error() {
        return error;
    }

    /** Las ranuras tal como las encontró la última actualización. */
    public List<VistaRanuraGuardado> ranuras() {
        return ranuras;
    }

    /** La línea de cada ranura, por ejemplo {@code Ranura 1: Año 1 · Verano · día 5 · 1.250 m · 2026-11-22 10:30}. */
    public List<String> lineas() {
        return ranuras.stream().map(PresentadorRanuras::linea).toList();
    }

    /** La línea de una ranura. */
    public static String linea(VistaRanuraGuardado ranura) {
        String prefijo = "Ranura " + ranura.ranura() + ": ";
        return switch (ranura.status()) {
            case "EMPTY" -> prefijo + Textos.RANURA_VACIA;
            case "DAMAGED" -> prefijo + Textos.RANURA_DANADA;
            default -> prefijo + Textos.fecha(ranura.anio(), ranura.estacion(), ranura.dia()).replace("Día", "día") + " · "
                    + Textos.dinero(ranura.dinero()) + " · " + ranura.archivadoEn();
        };
    }

    /** Verdadero si la ranura contiene una partida que se puede cargar. */
    public boolean puedeCargar(int ranura) {
        return ranuras.stream().anyMatch(s -> s.ranura() == ranura && "OK".equals(s.status()));
    }

    /** Verdadero si guardar en la ranura reemplazaría algo (se le pregunta primero al jugador, HU-29 · CA-3). */
    public boolean confirmacionNecesita(int ranura) {
        return ranuras.stream().anyMatch(s -> s.ranura() == ranura && !"EMPTY".equals(s.status()));
    }

    /**
     * Guarda en la ranura, preguntando primero si está ocupada.
     *
     * @param ranura      número de ranura
     * @param confirmado se le pregunta cuando la ranura está ocupada; el guardado sigue solo si responde {@code true}
     * @return el mensaje que se muestra, o un texto vacío si el jugador canceló
     */
    public String save(int ranura, BooleanSupplier confirmado) {
        if (confirmacionNecesita(ranura) && !confirmado.getAsBoolean()) {
            return "";
        }
        Resultado<VistaRanuraGuardado> resultado = juego.save(ranura);
        refrescar();
        return resultado.isOk() ? Textos.ARCHIVADO_EN_RANURA + ranura + "." : resultado.mensaje();
    }

    /** Carga la ranura; la partida en curso cambia solo si esto tiene éxito. */
    public Resultado<VistaHud> cargar(int ranura) {
        Resultado<VistaHud> resultado = juego.cargar(ranura);
        refrescar();
        return resultado;
    }
}
