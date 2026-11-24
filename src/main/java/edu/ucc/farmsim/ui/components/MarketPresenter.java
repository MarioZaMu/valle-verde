package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.controller.GameCommands;
import edu.ucc.farmsim.application.dto.AnimalOfferView;
import edu.ucc.farmsim.application.dto.BuildingView;
import edu.ucc.farmsim.application.dto.BuyOfferView;
import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.dto.MarketView;
import edu.ucc.farmsim.application.dto.SellOfferView;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Messages;
import edu.ucc.farmsim.application.result.Names;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.ui.theme.Texts;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * La lógica del diálogo del mercado, sin nada de Swing (HU-38): las filas de cada pestaña, el rango de cantidad, el
 * total previsto de una venta, si se puede pagar una compra y los mensajes de resultado. El diálogo solo la dibuja.
 *
 * <p>Stories: HU-14, HU-15, HU-16, HU-19, HU-38.
 */
public final class MarketPresenter {

    /** Mayor cantidad de una compra (RN-16). */
    public static final int MAX_BUY = 99;

    private final GameCommands game;
    private MarketView view = new MarketView(List.of(), List.of());
    private int money;

    /**
     * Crea el presentador.
     *
     * @param game la partida a la que pertenece el mercado
     */
    public MarketPresenter(GameCommands game) {
        this.game = game;
    }

    /** Carga otra vez los precios, las tendencias y el saldo. */
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

    /** El dinero que tiene ahora el jugador, como texto. */
    public String moneyText() {
        return Texts.money(money);
    }

    /** Los productos que se pueden vender. */
    public List<SellOfferView> sells() {
        return view.sells();
    }

    /** Los productos que se pueden comprar: semillas, heno y medicina, y luego los animales (HU-19). */
    public List<BuyOfferView> buys() {
        List<BuyOfferView> rows = new ArrayList<>(view.buys());
        view.animals().forEach(animal -> rows.add(new BuyOfferView(animal.animalId(), animal.name(), animal.price())));
        return rows;
    }

    /** Verdadero si el id es un animal y no un objeto. */
    public boolean isAnimal(String id) {
        return animal(id).isPresent();
    }

    /** El precio de una fila de venta como texto, con su tendencia como símbolo y palabra (HU-16 · CA-4). */
    public String sellPriceText(SellOfferView offer) {
        return Texts.money(offer.unitPrice()) + " " + Texts.trend(offer.trend());
    }

    /**
     * La mayor cantidad que permite el selector: de 1 a 99 al comprar, de 1 hasta las unidades que se tienen al vender.
     */
    public int maxQuantity(boolean buying, String itemId) {
        if (buying) {
            return isAnimal(itemId) ? 1 : MAX_BUY;
        }
        return sell(itemId).map(SellOfferView::owned).orElse(0);
    }

    /** El «Total previsto» de vender la cantidad ahora, o el mensaje de rechazo. */
    public String previewSale(String itemId, int quantity) {
        Result<Integer> quote = game.quote(itemId, quantity);
        return quote.isOk() ? "Total previsto: " + Texts.money(quote.value()) : quote.message();
    }

    /** Lo que costaría comprar, como texto. */
    public String previewPurchase(String itemId, int quantity) {
        return buy(itemId).map(offer -> "Total: " + Texts.money(offer.price() * quantity)).orElse("");
    }

    /** Si el jugador puede pagar la compra (el botón de comprar se deshabilita si no, HU-38 · CA-3). */
    public boolean canBuy(String itemId, int quantity) {
        return buy(itemId).map(offer -> quantity >= 1 && quantity <= maxQuantity(true, itemId)
                && offer.price() * quantity <= money && !noRoom(itemId)).orElse(false);
    }

    /** La razón por la que una compra está bloqueada, con las palabras de RN-35, o un texto vacío si se puede hacer. */
    public String purchaseBlock(String itemId, int quantity) {
        Optional<BuyOfferView> offer = buy(itemId);
        if (offer.isPresent() && quantity >= 1 && offer.get().price() * quantity > money) {
            return Messages.of(ErrorCode.SIN_FONDOS, offer.get().price() * quantity - money);
        }
        if (noRoom(itemId)) {
            return Messages.of(ErrorCode.SIN_ESPACIO_ANIMALES, Names.animalPlural(itemId));
        }
        return "";
    }

    /** Compra y responde el mensaje que se muestra; el mercado y el saldo se recargan. */
    public String buyUnits(String itemId, int quantity) {
        if (isAnimal(itemId)) {
            return buyAnimal(itemId);
        }
        Result<Integer> result = game.buy(itemId, quantity);
        String name = buy(itemId).map(BuyOfferView::name).orElse(itemId);
        refresh();
        return result.isOk() ? "Compraste " + quantity + " × " + name + " por " + Texts.money(result.value()) + "."
                : result.message();
    }

    /** Vende y responde el mensaje que se muestra; el mercado y el saldo se recargan. */
    public String sellUnits(String itemId, int quantity) {
        String name = sell(itemId).map(SellOfferView::name).orElse(itemId);
        Result<Integer> result = game.sell(itemId, quantity);
        refresh();
        return result.isOk() ? "Vendiste " + quantity + " × " + name + " por " + Texts.money(result.value()) + "."
                : result.message();
    }

    private String buyAnimal(String animalId) {
        String name = buy(animalId).map(BuyOfferView::name).orElse(animalId);
        Result<BuildingView> result = game.buyAnimal(animalId);
        refresh();
        return result.isOk() ? "Compraste: " + name + ". Vive en el " + result.value().name() + " "
                + (result.value().id() + 1) + "." : result.message();
    }

    private boolean noRoom(String id) {
        return animal(id).map(offer -> offer.freePlaces() <= 0).orElse(false);
    }

    private Optional<AnimalOfferView> animal(String id) {
        return view.animals().stream().filter(offer -> offer.animalId().equals(id)).findFirst();
    }

    private Optional<SellOfferView> sell(String itemId) {
        return view.sells().stream().filter(offer -> offer.itemId().equals(itemId)).findFirst();
    }

    private Optional<BuyOfferView> buy(String itemId) {
        return buys().stream().filter(offer -> offer.itemId().equals(itemId)).findFirst();
    }
}
