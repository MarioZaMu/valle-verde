package edu.ucc.farmsim.aplicacion.servicio;

import edu.ucc.farmsim.aplicacion.dto.VistaConversacion;
import edu.ucc.farmsim.aplicacion.resultado.Nombres;
import edu.ucc.farmsim.dominio.animal.Animal;
import edu.ucc.farmsim.dominio.cultivo.TipoCultivo;
import edu.ucc.farmsim.dominio.economia.Mercado;
import edu.ucc.farmsim.dominio.economia.Tendencia;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.edificio.Edificio;
import edu.ucc.farmsim.dominio.juego.EstadoJuego;
import edu.ucc.farmsim.dominio.npc.Npc;
import edu.ucc.farmsim.dominio.tiempo.Estacion;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Escribe lo que dice cada persona del valle (RN-37): Marta habla de precios, Don Tomás de qué sembrar en la estación,
 * la Dra. Lucía de los animales e Inés del clima de mañana, y cada una de su encargo (RN-38). El texto depende solo del
 * estado de la partida y no usa azar, así que el mismo estado siempre da las mismas palabras.
 *
 * <p>Stories: HU-46, HU-47.
 */
@Component
public final class ServicioConversacion {

    /** Lo que la persona le dice ahora al jugador. */
    public VistaConversacion conversacion(Npc npc, EstadoJuego estado) {
        String texto = switch (npc) {
            case MARTA -> precios(estado);
            case TOMAS -> siembra(estado);
            case LUCIA -> animales(estado);
            case INES -> pronostico(estado);
        };
        boolean hecho = estado.encargos().isDone(npc);
        String unidades = Nombres.conteoArticulo(npc.articuloEncargo().name(), npc.montoEncargo());
        String encargo = hecho
                ? "¡Gracias otra vez por traerme " + unidades + "!"
                : "Encargo: tráeme " + unidades + " y te pago " + npc.recompensaEncargo() + " m.";
        return new VistaConversacion(npc.name(), Nombres.npc(npc.name()), Nombres.rolNpc(npc.name()), texto, encargo, hecho);
    }

    private static String precios(EstadoJuego estado) {
        Estacion estacion = estado.fecha().estacion();
        Mercado mercado = estado.mercado();
        TipoArticulo mejor = null;
        TipoArticulo cayendo = null;
        TipoArticulo subiendo = null;
        for (TipoArticulo articulo : Mercado.articulosVendibles()) {
            if (mejor == null || mercado.precioDe(articulo, estacion) > mercado.precioDe(mejor, estacion)) {
                mejor = articulo;
            }
            Tendencia tendencia = mercado.tendenciaDe(articulo, estacion);
            if (cayendo == null && tendencia == Tendencia.DOWN) {
                cayendo = articulo;
            }
            if (subiendo == null && tendencia == Tendencia.UP) {
                subiendo = articulo;
            }
        }
        StringBuilder texto = new StringBuilder("¡Bienvenido al mercado! Hoy lo mejor pagado es ")
                .append(Nombres.articulo(mejor.name())).append(", a ").append(mercado.precioDe(mejor, estacion))
                .append(" m por unidad.");
        if (cayendo != null) {
            texto.append(' ').append(Nombres.articulo(cayendo.name()))
                    .append(" viene bajando de precio: si puedes, espera unos días para vender.");
        } else if (subiendo != null) {
            texto.append(' ').append(Nombres.articulo(subiendo.name())).append(" viene subiendo de precio.");
        } else {
            texto.append(" Los precios están tranquilos.");
        }
        return texto.toString();
    }

    private static String siembra(EstadoJuego estado) {
        Estacion estacion = estado.fecha().estacion();
        List<String> cultivos = new ArrayList<>();
        for (TipoCultivo cultivo : TipoCultivo.values()) {
            if (cultivo.estacionesValidos().contains(estacion)) {
                cultivos.add(Nombres.cultivo(cultivo.name()));
            }
        }
        String texto = cultivos.isEmpty()
                ? "En " + Nombres.estacion(estacion.name()) + " no crece nada al aire libre: usa un Invernadero."
                : "En " + Nombres.estacion(estacion.name()) + " conviene sembrar " + lista(cultivos) + ".";
        return texto + switch (estado.clima().hoy()) {
            case RAINY -> " Hoy llueve: la lluvia riega por ti.";
            case DROUGHT -> " Con la Sequía de hoy, regar cuesta el doble.";
            case FROST -> " La Helada de hoy marchita los cultivos al aire libre.";
            default -> "";
        };
    }

    private static String animales(EstadoJuego estado) {
        int total = 0;
        int enfermo = 0;
        int sinAlimentar = 0;
        for (Edificio edificio : estado.granja().edificios()) {
            for (Animal animal : edificio.animales()) {
                total++;
                if (animal.esEnfermo()) {
                    enfermo++;
                } else if (!animal.esHoyAlimentado()) {
                    sinAlimentar++;
                }
            }
        }
        if (total == 0) {
            return "Aún no tienes animales. Un Gallinero es un buen comienzo.";
        }
        if (enfermo > 0) {
            return enfermo == 1
                    ? "Tienes 1 animal enfermo: cúralo con Medicina antes de que empeore."
                    : "Tienes " + enfermo + " animales enfermos: cúralos con Medicina antes de que empeoren.";
        }
        if (sinAlimentar > 0) {
            return sinAlimentar == 1
                    ? "1 animal aún no ha comido hoy: dale Heno."
                    : sinAlimentar + " animales aún no han comido hoy: dales Heno.";
        }
        return "Tus animales están sanos y bien alimentados. ¡Buen trabajo!";
    }

    private static String pronostico(EstadoJuego estado) {
        return "El pronóstico para mañana es " + Nombres.clima(estado.clima().pronostico().name()) + "."
                + switch (estado.clima().pronostico()) {
                    case RAINY -> " No hará falta regar.";
                    case FROST -> " Cosecha lo que esté maduro: la helada marchita lo que está al aire libre.";
                    case DROUGHT -> " Regar costará el doble.";
                    case SNOWY -> " Abrígate bien.";
                    case CLOUDY -> " Será un día tranquilo.";
                    case SUNNY -> " ¡Un buen día para trabajar el campo!";
                };
    }

    private static String lista(List<String> palabras) {
        if (palabras.size() == 1) {
            return palabras.get(0);
        }
        return String.join(", ", palabras.subList(0, palabras.size() - 1)) + " y " + palabras.get(palabras.size() - 1);
    }
}
