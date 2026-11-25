package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.aplicacion.controlador.ComandosJuego;
import edu.ucc.farmsim.aplicacion.dto.VistaAccionEdificio;
import edu.ucc.farmsim.aplicacion.dto.VistaAnimal;
import edu.ucc.farmsim.aplicacion.dto.VistaEdificio;
import edu.ucc.farmsim.aplicacion.dto.VistaEdificios;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.interfaz.tema.Textos;
import java.util.List;

/**
 * La lógica del panel del edificio, sin nada de Swing (HU-20, HU-22, HU-23): los animales que se listan, qué botones
 * están habilitados y los mensajes después de alimentar, recoger o curar. El diálogo solo la dibuja.
 *
 * <p>Curar se hace aquí y no con una herramienta de la barra (RN-27).
 *
 * <p>Stories: HU-20, HU-22, HU-23.
 */
public final class PresentadorEdificio {

    private static final String SICK = "SICK";

    private final ComandosJuego juego;
    private final int idEdificio;
    private VistaEdificio edificio;

    /**
     * Crea el presentador y carga el edificio.
     *
     * @param juego       la partida a la que pertenece el edificio
     * @param idEdificio id del edificio
     */
    public PresentadorEdificio(ComandosJuego juego, int idEdificio) {
        this.juego = juego;
        this.idEdificio = idEdificio;
        refrescar();
    }

    /** Carga el edificio otra vez. */
    public void refrescar() {
        Resultado<VistaEdificios> todo = juego.edificios();
        if (todo.isOk()) {
            edificio = todo.value().edificios().stream().filter(b -> b.id() == idEdificio).findFirst()
                    .orElse(edificio);
        }
    }

    /** El edificio, o {@code null} si no se pudo cargar. */
    public VistaEdificio edificio() {
        return edificio;
    }

    /** Título del panel, por ejemplo {@code Gallinero 1 · 2/4 animales}. */
    public String titulo() {
        if (edificio == null) {
            return Textos.TITULO_EDIFICIO;
        }
        return edificio.name() + " " + (edificio.id() + 1) + " — " + edificio.animales().size() + "/"
                + edificio.capacidad() + " animales";
    }

    /** Los animales del edificio. */
    public List<VistaAnimal> animales() {
        return edificio == null ? List.of() : edificio.animales();
    }

    /** La fila de un animal tal como la muestra la tabla. */
    public Object[] fila(VistaAnimal animal) {
        return new Object[] {animal.name(), animal.nombreCondicion(), animal.diasSinAlimentar(),
                animal.hoyAlimentado() ? Textos.SI : Textos.NO, animal.pendiente()};
    }

    /** Verdadero si algún animal puede comer y no ha comido hoy. */
    public boolean puedeAlimentar() {
        return animales().stream().anyMatch(a -> !SICK.equals(a.condicion()) && !a.hoyAlimentado());
    }

    /** Verdadero si el edificio tiene productos esperando. */
    public boolean puedeRecolectar() {
        return edificio != null && edificio.productosPendientes() > 0;
    }

    /** Verdadero si el animal de la posición está enfermo. */
    public boolean puedeCurar(int indiceAnimal) {
        return indiceAnimal >= 0 && indiceAnimal < animales().size() && SICK.equals(animales().get(indiceAnimal).condicion());
    }

    /** Alimenta el edificio y responde el mensaje que se muestra. */
    public String alimentar() {
        Resultado<VistaAccionEdificio> resultado = juego.alimentar(idEdificio);
        refrescar();
        if (!resultado.isOk()) {
            return resultado.mensaje();
        }
        VistaAccionEdificio vista = resultado.value();
        return vista.notificacion() != null ? vista.notificacion() : "Alimentaste a " + vista.monto() + " animal(es).";
    }

    /** Recoge los productos y responde el mensaje que se muestra. */
    public String collect() {
        Resultado<VistaAccionEdificio> resultado = juego.collect(idEdificio);
        refrescar();
        if (!resultado.isOk()) {
            return resultado.mensaje();
        }
        VistaAccionEdificio vista = resultado.value();
        return vista.notificacion() != null ? vista.notificacion() : "Recogiste " + vista.monto() + " × " + producto(vista) + ".";
    }

    /** Cura al animal de la posición y responde el mensaje que se muestra. */
    public String curar(int indiceAnimal) {
        Resultado<VistaAccionEdificio> resultado = juego.curar(idEdificio, indiceAnimal);
        refrescar();
        if (!resultado.isOk()) {
            return resultado.mensaje();
        }
        VistaAccionEdificio vista = resultado.value();
        return vista.notificacion() != null ? vista.notificacion() : "El animal se curó.";
    }

    private static String producto(VistaAccionEdificio vista) {
        return vista.edificio().nombreProducto() == null ? "productos" : vista.edificio().nombreProducto();
    }
}
