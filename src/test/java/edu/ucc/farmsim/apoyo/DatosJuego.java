package edu.ucc.farmsim.apoyo;

import edu.ucc.farmsim.dominio.animal.Animal;
import edu.ucc.farmsim.dominio.animal.CondicionAnimal;
import edu.ucc.farmsim.dominio.animal.InstantaneaAnimal;
import edu.ucc.farmsim.dominio.animal.TipoAnimal;
import edu.ucc.farmsim.dominio.cultivo.Cultivo;
import edu.ucc.farmsim.dominio.cultivo.InstantaneaCultivo;
import edu.ucc.farmsim.dominio.cultivo.TipoCultivo;
import edu.ucc.farmsim.dominio.economia.InstantaneaInventario;
import edu.ucc.farmsim.dominio.economia.InstantaneaMercado;
import edu.ucc.farmsim.dominio.economia.Inventario;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.edificio.Edificio;
import edu.ucc.farmsim.dominio.edificio.TipoEdificio;
import edu.ucc.farmsim.dominio.juego.EstadoJuego;
import edu.ucc.farmsim.dominio.juego.MementoJuego;
import edu.ucc.farmsim.dominio.progreso.InstantaneaProgreso;
import edu.ucc.farmsim.dominio.progreso.Logro;
import edu.ucc.farmsim.dominio.tiempo.Estacion;
import edu.ucc.farmsim.dominio.tiempo.FechaJuego;
import edu.ucc.farmsim.dominio.tiempo.RelojJuego;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Ayudantes que dejan una partida en una situación conocida sin pasar por todo el flujo día a día. */
public final class DatosJuego {

    /** Una parcela de tierra dentro del bloque de tierra inicial. */
    public static final int COL_TIERRA = 5;
    public static final int FILA_TIERRA = 3;

    /** Parcela superior izquierda de un área de pasto libre donde cabe un edificio de 2 x 2 o 3 x 2. */
    public static final int COL_HIERBA = 2;
    public static final int FILA_HIERBA = 7;

    private DatosJuego() {
                // solo métodos de utilidad estáticos
    }

    /** Pone un cultivo con un progreso dado en una parcela, saltándose las reglas de siembra. */
    public static Cultivo plantar(EstadoJuego estado, int col, int fila, TipoCultivo tipo, int diasCrecidos,
                             int diasSinAgua, boolean regado, boolean marchito) {
        Cultivo cultivo = Cultivo.desdeInstantanea(new InstantaneaCultivo(tipo, diasCrecidos, diasSinAgua, regado, marchito));
        estado.granja().parcelaEn(col, fila).plantar(cultivo);
        return cultivo;
    }

    /** Pone un cultivo vivo, sin regar y que ha crecido el número de días dado. */
    public static Cultivo plantar(EstadoJuego estado, int col, int fila, TipoCultivo tipo, int diasCrecidos) {
        return plantar(estado, col, fila, tipo, diasCrecidos, 0, false, false);
    }

    /** Pone un edificio en el mapa sin pagarlo ni comprobar su desbloqueo. */
    public static Edificio build(EstadoJuego estado, TipoEdificio tipo, int col, int fila) {
        return estado.granja().colocar(tipo, col, fila);
    }

    /** Pone un gallinero sobre el pasto libre de abajo a la izquierda. */
    public static Edificio gallinero(EstadoJuego estado) {
        return build(estado, TipoEdificio.COOP, COL_HIERBA, FILA_HIERBA);
    }

    /** Aloja un animal en un edificio en la situación dada, saltándose la compra y la alimentación. */
    public static Animal casa(Edificio edificio, TipoAnimal tipo, CondicionAnimal condicion, int diasSinAlimentar,
                               boolean hoyAlimentado, int pendiente) {
        Animal animal = Animal.desdeInstantanea(new InstantaneaAnimal(tipo, condicion, diasSinAlimentar, hoyAlimentado, pendiente));
        if (!edificio.animalAgregar(animal)) {
            throw new IllegalStateException("The building has no room");
        }
        return animal;
    }

    /** Aloja una gallina sana que no ha comido. */
    public static Animal gallina(Edificio gallinero) {
        return casa(gallinero, TipoAnimal.HEN, CondicionAnimal.HEALTHY, 0, false, 0);
    }

    /** Concede un logro sin jugar para conseguirlo. */
    public static void desbloquear(EstadoJuego estado, Logro logro) {
        MementoJuego now = estado.mementoCrear();
        InstantaneaProgreso progreso = now.progreso();
        Set<Logro> desbloqueado = EnumSet.noneOf(Logro.class);
        desbloqueado.addAll(progreso.desbloqueado());
        desbloqueado.add(logro);
        if (cosechasPara(logro) > 1) {
            desbloqueado.add(Logro.FIRST_HARVEST);
        }
        if (logro == Logro.BIG_HARVESTER) {
            desbloqueado.add(Logro.GREEN_THUMB);
        }
                // mantiene coherentes los contadores con lo que está desbloqueado, como los tendría una partida real
        InstantaneaProgreso coherente = new InstantaneaProgreso(desbloqueado,
                Math.max(progreso.cosechas(), cosechasPara(logro)),
                Math.max(progreso.unidadesVendidos(), logro == Logro.TRADER ? 50 : 0),
                Math.max(progreso.edificiosConstruidos(), logro == Logro.BUILDER ? 1 : 0),
                Math.max(progreso.animalesCurados(), logro == Logro.VETERINARIAN ? 1 : 0));
        estado.restaurar(new MementoJuego(now.fecha(), now.dinero(), now.energia(), now.inventario(), now.granja(),
                now.hoy(), now.pronostico(), now.estadoAzar(), now.mercado(), now.libro(), coherente));
    }

    private static int cosechasPara(Logro logro) {
        return switch (logro) {
            case FIRST_HARVEST -> 1;
            case GREEN_THUMB -> 25;
            case BIG_HARVESTER -> 100;
            default -> 0;
        };
    }

    /** Fija la demanda de un producto (porcentaje) sin vender nada. */
    public static void demandaFijar(EstadoJuego estado, TipoArticulo articulo, int demanda) {
        InstantaneaMercado now = estado.mercado().instantanea();
        Map<TipoArticulo, Integer> cambiado = new EnumMap<>(TipoArticulo.class);
        cambiado.putAll(now.demanda());
        cambiado.put(articulo, demanda);
        estado.mercado().restaurarDesde(new InstantaneaMercado(cambiado, now.precioAnterior()));
    }

    /** Fija el dinero del jugador. */
    public static void dineroFijar(EstadoJuego estado, int dinero) {
        MementoJuego now = estado.mementoCrear();
        estado.restaurar(new MementoJuego(now.fecha(), dinero, now.energia(), now.inventario(), now.granja(), now.hoy(),
                now.pronostico(), now.estadoAzar(), now.mercado(), now.libro(), now.progreso()));
    }

    /** Mueve el calendario a una fecha. */
    public static void fechaFijar(int anio, Estacion estacion, int dia) {
        RelojJuego.getInstance().restaurar(new FechaJuego(anio, estacion, dia));
    }

    /**
     * Reemplaza el inventario por uno cuya primera casilla tiene la cantidad dada de un objeto y cuyas demás casillas
     * son todas pilas llenas de heno, de modo que no cabe nada más.
     */
    public static void salvoInventarioRellenar(EstadoJuego estado, TipoArticulo articulo, int monto) {
        int capacidad = estado.inventario().capacidad();
        List<Inventario.PilaArticulo> ranuras = new ArrayList<>();
        ranuras.add(new Inventario.PilaArticulo(articulo, monto));
        for (int i = 1; i < capacidad; i++) {
            ranuras.add(new Inventario.PilaArticulo(TipoArticulo.HAY, 99));
        }
        estado.inventario().restaurarDesde(new InstantaneaInventario(capacidad, ranuras));
    }
}

