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
 * The logic of the construction dialog, without any Swing (HU-18): the catalogue, why a building cannot be
 * built and the message after trying. The corner of the building is the plot that was clicked.
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
     * Creates the presenter and loads the catalogue.
     *
     * @param game the game
     * @param col  column of the plot that was clicked
     * @param row  row of the plot that was clicked
     */
    public ConstructionPresenter(GameCommands game, int col, int row) {
        this.game = game;
        this.col = col;
        this.row = row;
        refresh();
    }

    /** Loads the catalogue and the balance again. */
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

    /** What can be built. */
    public List<BuildingOfferView> catalog() {
        return catalog;
    }

    /** The text under the title, for example {@code Esquina en (2, 7)}. */
    public String placeText() {
        return "Esquina superior izquierda en (" + col + ", " + row + ")";
    }

    /** The money the player has now, as text. */
    public String moneyText() {
        return Texts.money(money);
    }

    /** The row of an offer as the table shows it. */
    public Object[] row(BuildingOfferView offer) {
        return new Object[] {offer.name(), Texts.size(offer.width(), offer.height()), Texts.money(offer.cost()),
                state(offer)};
    }

    /** What the state column says: locked (and why), the limit reached, or how many are built. */
    public String state(BuildingOfferView offer) {
        if (!offer.unlocked()) {
            return "Bloqueado: requiere el logro «" + offer.requirement() + "»";
        }
        return offer.built() + "/" + offer.max() + " construidos";
    }

    /** The reason the building cannot be built now, in the words of RN-35, or an empty text if it can. */
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

    /** Whether the build button is enabled for the offer. */
    public boolean canBuild(BuildingOfferView offer) {
        return block(offer).isEmpty();
    }

    /**
     * Builds at the clicked plot.
     *
     * @return the message to show; the result is available through {@link #lastBuilt()}
     */
    public String build(BuildingOfferView offer) {
        Result<BuildingView> result = game.build(offer.type(), col, row);
        refresh();
        lastBuilt = result.isOk() ? Optional.of(result.value()) : Optional.empty();
        return result.isOk() ? "Construiste: " + offer.name() + "." : result.message();
    }

    /** The building the last successful {@link #build} created. */
    public Optional<BuildingView> lastBuilt() {
        return lastBuilt;
    }
}
