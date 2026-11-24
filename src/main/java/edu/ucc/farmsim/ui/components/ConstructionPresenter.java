package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.dto.BuildingOfferView;
import edu.ucc.farmsim.application.dto.BuildingView;
import edu.ucc.farmsim.application.dto.BuildingsView;
import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Messages;
import edu.ucc.farmsim.application.result.Names;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.ui.theme.Texts;
import java.util.List;
import java.util.Optional;

/**
 * La lógica del diálogo de construcción, sin nada de Swing (HU-18): el catálogo, por qué no se puede construir un
 * edificio y el mensaje después de intentarlo. La esquina del edificio es la parcela en la que se hizo clic.
 *
 * <p>Stories: HU-18.
 */
public final class ConstructionPresenter {

    private final GameCommands game;
    private final int col;
    private final int row;
    private List<BuildingOfferView> catalog = List.of();
    private int money;
    private Optional<BuildingView> lastBuilt = Optional.empty();

    /**
     * Crea el presentador y carga el catálogo.
     *
     * @param game la partida
     * @param col  columna de la parcela en que se hizo clic
     * @param row  fila de la parcela en que se hizo clic
     */
    public ConstructionPresenter(GameCommands game, int col, int row) {
        this.game = game;
        this.col = col;
        this.row = row;
        refresh();
    }

    /** Carga otra vez el catálogo y el saldo. */
    public void refresh() {
        Result<BuildingsView> buildings = game.buildings();
        if (buildings.isOk()) {
            catalog = buildings.value().catalog();
        }
        Result<HudView> hud = game.hud();
        if (hud.isOk()) {
            money = hud.value().money();
        }
    }

    /** Lo que se puede construir. */
    public List<BuildingOfferView> catalog() {
        return catalog;
    }

    /** El texto bajo el título, por ejemplo {@code Esquina en (2, 7)}. */
    public String placeText() {
        return "Esquina superior izquierda en (" + col + ", " + row + ")";
    }

    /** El dinero que tiene ahora el jugador, como texto. */
    public String moneyText() {
        return Texts.money(money);
    }

    /** La fila de una oferta tal como la muestra la tabla. */
    public Object[] row(BuildingOfferView offer) {
        return new Object[] {offer.name(), Texts.size(offer.width(), offer.height()), Texts.money(offer.cost()),
                state(offer)};
    }

    /** Lo que dice la columna de estado: bloqueado (y por qué), el límite alcanzado, o cuántos hay construidos. */
    public String state(BuildingOfferView offer) {
        if (!offer.unlocked()) {
            return "Bloqueado: requiere el logro «" + offer.requirement() + "»";
        }
        return offer.built() + "/" + offer.max() + " construidos";
    }

    /**
     * La razón por la que ahora no se puede construir el edificio, con las palabras de RN-35, o un texto vacío si se
     * puede.
     */
    public String block(BuildingOfferView offer) {
        if (!offer.unlocked()) {
            return Messages.of(ErrorCode.EDIFICIO_BLOQUEADO, offer.requirement());
        }
        if (offer.built() >= offer.max()) {
            return Messages.of(ErrorCode.LIMITE_EDIFICIOS, Names.buildingPlural(offer.type()));
        }
        if (offer.cost() > money) {
            return Messages.of(ErrorCode.SIN_FONDOS, offer.cost() - money);
        }
        return "";
    }

    /** Si el botón de construir está habilitado para la oferta. */
    public boolean canBuild(BuildingOfferView offer) {
        return block(offer).isEmpty();
    }

    /**
     * Construye en la parcela donde se hizo clic.
     *
     * @return el mensaje que se muestra; el resultado está disponible mediante {@link #lastBuilt()}
     */
    public String build(BuildingOfferView offer) {
        Result<BuildingView> result = game.build(offer.type(), col, row);
        refresh();
        lastBuilt = result.isOk() ? Optional.of(result.value()) : Optional.empty();
        return result.isOk() ? "Construiste: " + offer.name() + "." : result.message();
    }

    /** El edificio que creó el último {@link #build} exitoso. */
    public Optional<BuildingView> lastBuilt() {
        return lastBuilt;
    }
}
