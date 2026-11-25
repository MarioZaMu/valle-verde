package edu.ucc.farmsim.apoyo;

import edu.ucc.farmsim.dominio.guardado.ExcepcionAlmacenamientoGuardado;
import edu.ucc.farmsim.dominio.guardado.ExcepcionCargarGuardado;
import edu.ucc.farmsim.dominio.guardado.InfoRanura;
import edu.ucc.farmsim.dominio.guardado.RepositorioGuardado;
import edu.ucc.farmsim.dominio.juego.MementoJuego;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sustituto en memoria del repositorio de partidas. Las pruebas pueden decir qué ranuras «existen», que una ranura está
 * dañada, o que el almacenamiento está caído; guardar y cargar realmente conservan la partida, así que se puede
 * comprobar un viaje de ida y vuelta sin MongoDB.
 */
public final class RepositorioGuardadoFalso implements RepositorioGuardado {

    private record Almacenado(MementoJuego memento, Instant archivadoEn) { }

    private final Map<Integer, Almacenado> almacenado = new HashMap<>();
    private final Set<Integer> ocupado = new HashSet<>();
    private final Set<Integer> danado = new HashSet<>();
    private boolean abajo;

    /** Marca una ranura como ocupada (su contenido no se puede cargar). */
    public RepositorioGuardadoFalso conRanura(int ranura) {
        ocupado.add(ranura);
        return this;
    }

    /** Marca una ranura como con un documento dañado. */
    public RepositorioGuardadoFalso conRanuraDanada(int ranura) {
        danado.add(ranura);
        return this;
    }

    /** Hace que toda llamada falle como si MongoDB no respondiera. */
    public RepositorioGuardadoFalso inalcanzable() {
        this.abajo = true;
        return this;
    }

    /** Devuelve el almacenamiento. */
    public void alcanzable() {
        this.abajo = false;
    }

    /** Olvida todas las ranuras y devuelve el almacenamiento (para las pruebas que comparten un repositorio). */
    public void reiniciar() {
        almacenado.clear();
        ocupado.clear();
        danado.clear();
        abajo = false;
    }

    private void arribaComprobar() {
        if (abajo) {
            throw new ExcepcionAlmacenamientoGuardado("MongoDB no respondió", new IllegalStateException("simulated outage"));
        }
    }

    @Override
    public boolean existeRanura(int ranura) {
        arribaComprobar();
        return ocupado.contains(ranura) || danado.contains(ranura) || almacenado.containsKey(ranura);
    }

    @Override
    public void guardar(int ranura, MementoJuego memento, Instant archivadoEn) {
        arribaComprobar();
        almacenado.put(ranura, new Almacenado(memento, archivadoEn));
        danado.remove(ranura);
    }

    @Override
    public MementoJuego cargar(int ranura) {
        arribaComprobar();
        if (danado.contains(ranura)) {
            throw new ExcepcionCargarGuardado(ExcepcionCargarGuardado.Motivo.CORRUPT, "simulated damage");
        }
        Almacenado encontrado = almacenado.get(ranura);
        if (encontrado == null) {
            throw new ExcepcionCargarGuardado(ExcepcionCargarGuardado.Motivo.MISSING, "slot " + ranura);
        }
        return encontrado.memento();
    }

    @Override
    public List<InfoRanura> ranuras() {
        arribaComprobar();
        List<InfoRanura> ranuras = new ArrayList<>();
        for (int ranura = 1; ranura <= 3; ranura++) {
            Almacenado encontrado = almacenado.get(ranura);
            if (danado.contains(ranura)) {
                ranuras.add(InfoRanura.danado(ranura));
            } else if (encontrado != null) {
                ranuras.add(InfoRanura.ok(ranura, encontrado.memento().fecha(), encontrado.memento().dinero(), encontrado.archivadoEn()));
            } else {
                ranuras.add(InfoRanura.empty(ranura));
            }
        }
        return ranuras;
    }
}
