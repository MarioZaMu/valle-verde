package edu.ucc.farmsim.application.service;

import edu.ucc.farmsim.application.dto.DialogueView;
import edu.ucc.farmsim.application.result.Names;
import edu.ucc.farmsim.domain.animal.Animal;
import edu.ucc.farmsim.domain.building.Building;
import edu.ucc.farmsim.domain.crop.CropType;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.economy.Market;
import edu.ucc.farmsim.domain.economy.Trend;
import edu.ucc.farmsim.domain.game.GameState;
import edu.ucc.farmsim.domain.npc.Npc;
import edu.ucc.farmsim.domain.time.Season;
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
public final class DialogueService {

    /** Lo que la persona le dice ahora al jugador. */
    public DialogueView dialogue(Npc npc, GameState state) {
        String text = switch (npc) {
            case MARTA -> prices(state);
            case TOMAS -> sowing(state);
            case LUCIA -> animals(state);
            case INES -> forecast(state);
        };
        boolean done = state.errands().isDone(npc);
        String units = Names.itemCount(npc.errandItem().name(), npc.errandAmount());
        String errand = done
                ? "¡Gracias otra vez por traerme " + units + "!"
                : "Encargo: tráeme " + units + " y te pago " + npc.errandReward() + " m.";
        return new DialogueView(npc.name(), Names.npc(npc.name()), Names.npcRole(npc.name()), text, errand, done);
    }

    private static String prices(GameState state) {
        Season season = state.date().season();
        Market market = state.market();
        ItemType best = null;
        ItemType falling = null;
        ItemType rising = null;
        for (ItemType item : Market.sellableItems()) {
            if (best == null || market.priceOf(item, season) > market.priceOf(best, season)) {
                best = item;
            }
            Trend trend = market.trendOf(item, season);
            if (falling == null && trend == Trend.DOWN) {
                falling = item;
            }
            if (rising == null && trend == Trend.UP) {
                rising = item;
            }
        }
        StringBuilder text = new StringBuilder("¡Bienvenido al mercado! Hoy lo mejor pagado es ")
                .append(Names.item(best.name())).append(", a ").append(market.priceOf(best, season))
                .append(" m por unidad.");
        if (falling != null) {
            text.append(' ').append(Names.item(falling.name()))
                    .append(" viene bajando de precio: si puedes, espera unos días para vender.");
        } else if (rising != null) {
            text.append(' ').append(Names.item(rising.name())).append(" viene subiendo de precio.");
        } else {
            text.append(" Los precios están tranquilos.");
        }
        return text.toString();
    }

    private static String sowing(GameState state) {
        Season season = state.date().season();
        List<String> crops = new ArrayList<>();
        for (CropType crop : CropType.values()) {
            if (crop.validSeasons().contains(season)) {
                crops.add(Names.crop(crop.name()));
            }
        }
        String text = crops.isEmpty()
                ? "En " + Names.season(season.name()) + " no crece nada al aire libre: usa un Invernadero."
                : "En " + Names.season(season.name()) + " conviene sembrar " + list(crops) + ".";
        return text + switch (state.weather().today()) {
            case RAINY -> " Hoy llueve: la lluvia riega por ti.";
            case DROUGHT -> " Con la Sequía de hoy, regar cuesta el doble.";
            case FROST -> " La Helada de hoy marchita los cultivos al aire libre.";
            default -> "";
        };
    }

    private static String animals(GameState state) {
        int total = 0;
        int sick = 0;
        int unfed = 0;
        for (Building building : state.farm().buildings()) {
            for (Animal animal : building.animals()) {
                total++;
                if (animal.isSick()) {
                    sick++;
                } else if (!animal.isFedToday()) {
                    unfed++;
                }
            }
        }
        if (total == 0) {
            return "Aún no tienes animales. Un Gallinero es un buen comienzo.";
        }
        if (sick > 0) {
            return sick == 1
                    ? "Tienes 1 animal enfermo: cúralo con Medicina antes de que empeore."
                    : "Tienes " + sick + " animales enfermos: cúralos con Medicina antes de que empeoren.";
        }
        if (unfed > 0) {
            return unfed == 1
                    ? "1 animal aún no ha comido hoy: dale Heno."
                    : unfed + " animales aún no han comido hoy: dales Heno.";
        }
        return "Tus animales están sanos y bien alimentados. ¡Buen trabajo!";
    }

    private static String forecast(GameState state) {
        return "El pronóstico para mañana es " + Names.weather(state.weather().forecast().name()) + "."
                + switch (state.weather().forecast()) {
                    case RAINY -> " No hará falta regar.";
                    case FROST -> " Cosecha lo que esté maduro: la helada marchita lo que está al aire libre.";
                    case DROUGHT -> " Regar costará el doble.";
                    case SNOWY -> " Abrígate bien.";
                    case CLOUDY -> " Será un día tranquilo.";
                    case SUNNY -> " ¡Un buen día para trabajar el campo!";
                };
    }

    private static String list(List<String> words) {
        if (words.size() == 1) {
            return words.get(0);
        }
        return String.join(", ", words.subList(0, words.size() - 1)) + " y " + words.get(words.size() - 1);
    }
}
