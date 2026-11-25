package edu.ucc.farmsim.aplicacion.servicio;

import edu.ucc.farmsim.aplicacion.dto.VistaAnimal;
import edu.ucc.farmsim.aplicacion.dto.VistaAvatar;
import edu.ucc.farmsim.aplicacion.dto.VistaCasa;
import edu.ucc.farmsim.aplicacion.dto.VistaEdificio;
import edu.ucc.farmsim.aplicacion.dto.VistaEdificios;
import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.dto.VistaMapa;
import edu.ucc.farmsim.aplicacion.dto.VistaNpc;
import edu.ucc.farmsim.aplicacion.dto.VistaOfertaEdificio;
import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
import edu.ucc.farmsim.aplicacion.dto.VistaResumenDia;
import edu.ucc.farmsim.aplicacion.resultado.Mensajes;
import edu.ucc.farmsim.aplicacion.resultado.Nombres;
import edu.ucc.farmsim.dominio.animal.Animal;
import edu.ucc.farmsim.dominio.cultivo.Cultivo;
import edu.ucc.farmsim.dominio.edificio.Edificio;
import edu.ucc.farmsim.dominio.edificio.TipoEdificio;
import edu.ucc.farmsim.dominio.granja.Avatar;
import edu.ucc.farmsim.dominio.granja.Casa;
import edu.ucc.farmsim.dominio.granja.Parcela;
import edu.ucc.farmsim.dominio.juego.EstadoJuego;
import edu.ucc.farmsim.dominio.juego.ReporteDia;
import edu.ucc.farmsim.dominio.npc.Npc;
import edu.ucc.farmsim.dominio.progreso.Logro;
import edu.ucc.farmsim.dominio.tiempo.FechaJuego;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Convierte el estado del dominio en las vistas inmutables que dibuja la UI, de modo que la UI nunca ve objetos del
 * dominio.
 *
 * <p>Stories: HU-01, HU-17, HU-18, HU-28, HU-34, HU-35, HU-45, HU-46, HU-50.
 */
@Component
public final class MapeadorVista {

    /** Las casas nunca se mueven, así que sus vistas se hacen una sola vez. */
    private static final List<VistaCasa> CASAS = java.util.Arrays.stream(Casa.values())
            .map(h -> new VistaCasa(h.name(), h.col(), h.fila(), h.width(), h.height())).toList();

    /** La barra superior de un estado de partida. */
    public VistaHud hud(EstadoJuego estado) {
        FechaJuego fecha = estado.fecha();
        return new VistaHud(estado.dinero(), fecha.anio(), fecha.estacion().name(), fecha.dia(),
                estado.clima().hoy().name(), estado.clima().pronostico().name(),
                estado.energia().actual(), estado.energia().max());
    }

    /** La parcela en las coordenadas dadas, que deben estar dentro del mapa. */
    public VistaParcela parcela(EstadoJuego estado, int col, int fila) {
        Parcela parcela = estado.granja().parcelaEn(col, fila);
        Cultivo cultivo = parcela.cultivo();
        if (cultivo == null) {
            return new VistaParcela(col, fila, parcela.terreno().name(), null, null, false, 0, parcela.esResguardado());
        }
        return new VistaParcela(col, fila, parcela.terreno().name(), cultivo.tipo().name(), cultivo.etapa().name(),
                cultivo.esRegado(), cultivo.izquierdaDias(), parcela.esResguardado());
    }

    /** Todo el mapa de la granja, parcela por parcela en orden de fila. */
    public VistaMapa map(EstadoJuego estado) {
        int cols = estado.granja().cols();
        int filas = estado.granja().filas();
        List<VistaParcela> parcelas = new ArrayList<>();
        for (int fila = 0; fila < filas; fila++) {
            for (int col = 0; col < cols; col++) {
                parcelas.add(parcela(estado, col, fila));
            }
        }
        return new VistaMapa(cols, filas, List.copyOf(parcelas), vistasEdificio(estado), avatar(estado), npcs(estado), CASAS);
    }

    /** Las personas del valle, en un orden fijo, con el estado de sus encargos. */
    public List<VistaNpc> npcs(EstadoJuego estado) {
        List<VistaNpc> todo = new ArrayList<>();
        for (Npc npc : Npc.values()) {
            todo.add(new VistaNpc(npc.name(), Nombres.npc(npc.name()), Nombres.rolNpc(npc.name()), npc.col(), npc.fila(),
                    npc.articuloEncargo().name(), npc.montoEncargo(), npc.recompensaEncargo(), estado.encargos().isDone(npc)));
        }
        return List.copyOf(todo);
    }

    /** Dónde está el avatar y hacia dónde mira. */
    public VistaAvatar avatar(EstadoJuego estado) {
        Avatar avatar = estado.avatar();
        return new VistaAvatar(avatar.col(), avatar.fila(), avatar.mirada().name());
    }

    /** Un edificio con sus animales. */
    public VistaEdificio edificio(Edificio edificio) {
        List<VistaAnimal> animales = new ArrayList<>();
        int indice = 0;
        for (Animal animal : edificio.animales()) {
            animales.add(new VistaAnimal(indice++, animal.tipo().name(), Nombres.animal(animal.tipo().name()),
                    animal.condicion().name(), Nombres.condicion(animal.condicion().name()),
                    animal.diasSinAlimentar(), animal.esHoyAlimentado(), animal.pendiente()));
        }
        String producto = edificio.producto() == null ? null : Nombres.articulo(edificio.producto().name());
        return new VistaEdificio(edificio.id(), edificio.tipo().name(), Nombres.edificio(edificio.tipo().name()),
                edificio.col(), edificio.fila(), edificio.width(), edificio.height(), edificio.capacidadAnimal(),
                producto, edificio.productosPendientes(), List.copyOf(animales));
    }

    /** Todos los edificios de la granja, en el orden en que se construyeron. */
    public List<VistaEdificio> vistasEdificio(EstadoJuego estado) {
        return estado.granja().edificios().stream().map(this::edificio).toList();
    }

    /** Los edificios y el catálogo de lo que aún se puede construir. */
    public VistaEdificios edificios(EstadoJuego estado) {
        List<VistaOfertaEdificio> catalogo = new ArrayList<>();
        for (TipoEdificio tipo : TipoEdificio.values()) {
            Logro requerido = tipo.requisito();
            boolean desbloqueado = requerido == null || estado.progreso().has(requerido);
            catalogo.add(new VistaOfertaEdificio(tipo.name(), Nombres.edificio(tipo.name()), tipo.width(), tipo.height(),
                    tipo.costo(), estado.granja().contarEdificios(tipo), tipo.conteoMaximo(), desbloqueado,
                    requerido == null ? null : Nombres.logro(requerido.name())));
        }
        return new VistaEdificios(vistasEdificio(estado), List.copyOf(catalogo));
    }

    /** El resumen del día que acaba de terminar. */
    public VistaResumenDia resumen(EstadoJuego estado, ReporteDia reporte) {
        List<String> luces = new ArrayList<>();
        if (reporte.cultivosMarchitos() > 0) {
            luces.add(reporte.cultivosMarchitos() == 1 ? "Se marchitó 1 cultivo."
                    : "Se marchitaron " + reporte.cultivosMarchitos() + " cultivos.");
        }
        if (reporte.animalesEnfermos() > 0) {
            luces.add(reporte.animalesEnfermos() == 1 ? "1 animal enfermó por falta de alimento."
                    : reporte.animalesEnfermos() + " animales enfermaron por falta de alimento.");
        }
        if (reporte.productos() > 0) {
            luces.add(reporte.productos() == 1 ? "Los animales produjeron 1 unidad."
                    : "Los animales produjeron " + reporte.productos() + " unidades.");
        }
        for (Logro logro : reporte.desbloqueado()) {
            luces.add("Logro desbloqueado: «" + Nombres.logro(logro.name()) + "» (+"
                    + logro.recompensa() + " m).");
        }
        if (luces.isEmpty()) {
            luces.add(Mensajes.NO_NOTICIAS);
        }
        FechaJuego dia = reporte.fechaAnterior() != null ? reporte.fechaAnterior() : estado.fecha();
        return new VistaResumenDia(dia.anio(), dia.estacion().name(), dia.dia(), reporte.ingresos(), reporte.gastos(),
                reporte.antesSaldo(), reporte.despuesSaldo(), List.copyOf(luces));
    }
}

