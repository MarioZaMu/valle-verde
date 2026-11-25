package edu.ucc.farmsim.dominio.economia;

import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Almacenamiento limitado: un número fijo de casillas, cada una con un tipo de objeto en una pila de hasta {@link
 * ReglasJuego#PILA_MAXIMA} unidades (RN-15).
 *
 * <p>La inserción es atómica: si no cabe toda la cantidad, no se inserta nada. Las operaciones del dominio señalan un
 * rechazo normal con {@code false}; la capa de aplicación lo convierte en el error {@code INVENTARIO_LLENO}. Una
 * cantidad no positiva es un error de programación y lanza {@link IllegalArgumentException}.
 *
 * <p>Stories: HU-10, HU-13.
 */
public final class Inventario {

    /** Una casilla ocupada. */
    public record PilaArticulo(TipoArticulo tipo, int monto) { }

    private TipoArticulo[] tipos;
    private int[] montos;

    /**
     * Crea un inventario vacío.
     *
     * @param capacidad número de casillas
     */
    public Inventario(int capacidad) {
        if (capacidad <= 0) {
            throw new IllegalArgumentException("capacity must be positive: " + capacidad);
        }
        this.tipos = new TipoArticulo[capacidad];
        this.montos = new int[capacidad];
    }

    /** Crea un inventario vacío con el número base de casillas (RN-15). */
    public static Inventario estandar() {
        return new Inventario(ReglasJuego.RANURAS_BASE_INVENTARIO);
    }

    /** Número de casillas. */
    public int capacidad() {
        return tipos.length;
    }

    /**
     * Agrega casillas vacías (un almacén, RN-20). Nada de lo ya guardado se mueve.
     *
     * @param extra cuántas casillas agregar
     */
    public void ranurasAgregar(int extra) {
        exigirPositivo(extra);
        tipos = Arrays.copyOf(tipos, tipos.length + extra);
        montos = Arrays.copyOf(montos, montos.length + extra);
    }

    /** Número de casillas ocupadas actualmente. */
    public int ranurasUsados() {
        int usado = 0;
        for (int monto : montos) {
            if (monto > 0) {
                usado++;
            }
        }
        return usado;
    }

    /** Total de unidades del objeto dado en todas las casillas. */
    public int count(TipoArticulo tipo) {
        int total = 0;
        for (int i = 0; i < tipos.length; i++) {
            if (tipos[i] == tipo) {
                total += montos[i];
            }
        }
        return total;
    }

    /** Casillas ocupadas en orden de casilla. */
    public List<PilaArticulo> pilas() {
        List<PilaArticulo> resultado = new ArrayList<>();
        for (int i = 0; i < tipos.length; i++) {
            if (montos[i] > 0) {
                resultado.add(new PilaArticulo(tipos[i], montos[i]));
            }
        }
        return Collections.unmodifiableList(resultado);
    }

    /**
     * Inserta unidades, completando primero las pilas del mismo tipo y usando después las casillas vacías.
     *
     * @return {@code true} si se insertó todo; {@code false} (y no se inserta nada) si no cabe
     */
    public boolean add(TipoArticulo tipo, int monto) {
        exigirPositivo(monto);
        if (espacioPara(tipo) < monto) {
            return false;
        }
        int restante = monto;
        for (int i = 0; i < tipos.length && restante > 0; i++) {
            if (tipos[i] == tipo && montos[i] > 0) {
                int movido = Math.min(restante, ReglasJuego.PILA_MAXIMA - montos[i]);
                montos[i] += movido;
                restante -= movido;
            }
        }
        for (int i = 0; i < tipos.length && restante > 0; i++) {
            if (montos[i] == 0) {
                int movido = Math.min(restante, ReglasJuego.PILA_MAXIMA);
                tipos[i] = tipo;
                montos[i] = movido;
                restante -= movido;
            }
        }
        return true;
    }

    /**
     * Quita unidades, empezando por la última pila de ese tipo.
     *
     * @return {@code true} si se quitó; {@code false} (y no se quita nada) si hay menos unidades
     */
    public boolean remove(TipoArticulo tipo, int monto) {
        exigirPositivo(monto);
        if (count(tipo) < monto) {
            return false;
        }
        int restante = monto;
        for (int i = tipos.length - 1; i >= 0 && restante > 0; i--) {
            if (tipos[i] == tipo && montos[i] > 0) {
                int tomado = Math.min(restante, montos[i]);
                montos[i] -= tomado;
                restante -= tomado;
                if (montos[i] == 0) {
                    tipos[i] = null;
                }
            }
        }
        return true;
    }

    /** Una copia inmutable de todas las casillas, incluidas las vacías, para el memento. */
    public InstantaneaInventario instantanea() {
        List<PilaArticulo> ranuras = new ArrayList<>();
        for (int i = 0; i < tipos.length; i++) {
            ranuras.add(new PilaArticulo(tipos[i], montos[i]));
        }
        return new InstantaneaInventario(tipos.length, List.copyOf(ranuras));
    }

    /** Reemplaza todo el contenido (y el número de casillas) con una instantánea tomada antes. */
    public void restaurarDesde(InstantaneaInventario instantanea) {
        this.tipos = new TipoArticulo[instantanea.capacidad()];
        this.montos = new int[instantanea.capacidad()];
        for (int i = 0; i < instantanea.ranuras().size(); i++) {
            PilaArticulo ranura = instantanea.ranuras().get(i);
            tipos[i] = ranura.monto() > 0 ? ranura.tipo() : null;
            montos[i] = ranura.monto();
        }
    }

    private int espacioPara(TipoArticulo tipo) {
        int espacio = 0;
        for (int i = 0; i < tipos.length; i++) {
            if (montos[i] == 0) {
                espacio += ReglasJuego.PILA_MAXIMA;
            } else if (tipos[i] == tipo) {
                espacio += ReglasJuego.PILA_MAXIMA - montos[i];
            }
        }
        return espacio;
    }

    private static void exigirPositivo(int monto) {
        if (monto <= 0) {
            throw new IllegalArgumentException("amount must be positive: " + monto);
        }
    }
}
