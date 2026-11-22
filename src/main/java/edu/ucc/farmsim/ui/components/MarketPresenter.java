package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.dto.BuyOfferView;
import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.dto.MarketView;
import edu.ucc.farmsim.application.dto.SellOfferView;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Messages;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.ui.theme.Texts;
import java.util.List;
import java.util.Optional;

/**
 * The logic of the market dialog, without any Swing (HU-38): the rows of each tab, the quantity range, the
 * forecast total of a sale, whether a purchase can be afforded and the result messages. The dialog only draws it.
 *
 * <p>Stories: HU-14, HU-15, HU-16, HU-38.
 */
public final class MarketPresenter {

    /** Largest quantity of a purchase (RN-16). */
    public static final int MAX_BUY = 99;

    private final GameCommands game;
    private MarketView view = new MarketView(List.of(), List.of());
    private int money;

    /**
     * Creates the presenter.
     *
     * @param game the game the market belongs to
     */
    public MarketPresenter(GameCommands game) {
        this.game = game;
    }

    /** Loads prices, trends and the balance again. */
    public void refresh() {
        Result<MarketView> market = game.market();
        if (market.isOk()) {
            view = market.value();
        }
        Result<HudView> hud = game.hud();
        if (hud.isOk()) {
            money = hud.value().money();
        }
    }

    /** The money the player has now, as text. */
    public String moneyText() {
        return Texts.money(money);
    }

    /** The products that can be sold. */
    public List<SellOfferView> sells() {
        return view.sells();
    }

    /** The products that can be bought. */
    public List<BuyOfferView> buys() {
        return view.buys();
    }

    /** The price of a sale row as text, with its trend as symbol and word (HU-16 · CA-4). */
    public String sellPriceText(SellOfferView offer) {
        return Texts.money(offer.unitPrice()) + " " + Texts.trend(offer.trend());
    }

    /** The largest quantity the selector allows: 1 to 99 when buying, 1 up to the units owned when selling. */
    public int maxQuantity(boolean buying, String itemId) {
        if (buying) {
            return MAX_BUY;
        }
        return sell(itemId).map(SellOfferView::owned).orElse(0);
    }

    /** "Total previsto" of selling the quantity now, or the rejection message. */
    public String previewSale(String itemId, int quantity) {
        Result<Integer> quote = game.quote(itemId, quantity);
        return quote.isOk() ? "Total previsto: " + Texts.money(quote.value()) : quote.message();
    }

    /** What buying would cost, as text. */
    public String previewPurchase(String itemId, int quantity) {
        return buy(itemId).map(offer -> "Total: " + Texts.money(offer.price() * quantity)).orElse("");
    }

    /** Whether the player can pay for the purchase (the buy button is disabled when not, HU-38 · CA-3). */
    public boolean canBuy(String itemId, int quantity) {
        return buy(itemId).map(offer -> quantity >= 1 && quantity <= MAX_BUY && offer.price() * quantity <= money)
                .orElse(false);
    }

    /** The reason a purchase is blocked, in the words of RN-35, or an empty text if it can be made. */
    public String purchaseBlock(String itemId, int quantity) {
        Optional<BuyOfferView> offer = buy(itemId);
        if (offer.isPresent() && quantity >= 1 && offer.get().price() * quantity > money) {
            return Messages.of(ErrorCode.SIN_FONDOS, offer.get().price() * quantity - money);
        }
        return "";
    }

    /** Buys and answers the message to show; the market and the balance are reloaded. */
    public String buyUnits(String itemId, int quantity) {
        Result<Integer> result = game.buy(itemId, quantity);
        String name = buy(itemId).map(BuyOfferView::name).orElse(itemId);
        refresh();
        return result.isOk() ? "Compraste " + quantity + " × " + name + " por " + Texts.money(result.value()) + "."
                : result.message();
    }

    /** Sells and answers the message to show; the market and the balance are reloaded. */
    public String sellUnits(String itemId, int quantity) {
        String name = sell(itemId).map(SellOfferView::name).orElse(itemId);
        Result<Integer> result = game.sell(itemId, quantity);
        refresh();
        return result.isOk() ? "Vendiste " + quantity + " × " + name + " por " + Texts.money(result.value()) + "."
                : result.message();
    }

    private Optional<SellOfferView> sell(String itemId) {
        return view.sells().stream().filter(offer -> offer.itemId().equals(itemId)).findFirst();
    }

    private Optional<BuyOfferView> buy(String itemId) {
        return view.buys().stream().filter(offer -> offer.itemId().equals(itemId)).findFirst();
    }
}
