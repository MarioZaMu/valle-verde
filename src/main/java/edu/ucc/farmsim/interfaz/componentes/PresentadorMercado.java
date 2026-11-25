package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.aplicacion.controlador.ComandosJuego;
import edu.ucc.farmsim.aplicacion.dto.VistaEdificio;
import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.dto.VistaMercado;
import edu.ucc.farmsim.aplicacion.dto.VistaOfertaAnimal;
import edu.ucc.farmsim.aplicacion.dto.VistaOfertaComprar;
import edu.ucc.farmsim.aplicacion.dto.VistaOfertaVender;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Mensajes;
import edu.ucc.farmsim.aplicacion.resultado.Nombres;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.interfaz.tema.Textos;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * La lógica del diálogo del mercado, sin nada de Swing (HU-38): las filas de cada pestaña, el rango de cantidad, el
 * total previsto de una venta, si se puede pagar una compra y los mensajes de resultado. El diálogo solo la dibuja.
 *
 * <p>Stories: HU-14, HU-15, HU-16, HU-19, HU-38.
 */
public final class PresentadorMercado {

    /** Mayor cantidad de una compra (RN-16). */
    public static final int COMPRAR_MAXIMO = 99;

    private final ComandosJuego juego;
    private VistaMercado vista = new VistaMercado(List.of(), List.of());
    private int dinero;

    /**
     * Crea el presentador.
     *
     * @param juego la partida a la que pertenece el mercado
     */
    public PresentadorMercado(ComandosJuego juego) {
        this.juego = juego;
    }

    /** Carga otra vez los precios, las tendencias y el saldo. */
    public void refrescar() {
        Resultado<VistaMercado> mercado = juego.mercado();
        if (mercado.isOk()) {
            vista = mercado.value();
        }
        Resultado<VistaHud> hud = juego.hud();
        if (hud.isOk()) {
            dinero = hud.value().dinero();
        }
    }

    /** El dinero que tiene ahora el jugador, como texto. */
    public String textoDinero() {
        return Textos.dinero(dinero);
    }

    /** Los productos que se pueden vender. */
    public List<VistaOfertaVender> vende() {
        return vista.vende();
    }

    /** Los productos que se pueden comprar: semillas, heno y medicina, y luego los animales (HU-19). */
    public List<VistaOfertaComprar> compra() {
        List<VistaOfertaComprar> filas = new ArrayList<>(vista.compra());
        vista.animales().forEach(animal -> filas.add(new VistaOfertaComprar(animal.animalId(), animal.name(), animal.precio())));
        return filas;
    }

    /** Verdadero si el id es un animal y no un objeto. */
    public boolean esAnimal(String id) {
        return animal(id).isPresent();
    }

    /** El precio de una fila de venta como texto, con su tendencia como símbolo y palabra (HU-16 · CA-4). */
    public String textoPrecioVender(VistaOfertaVender oferta) {
        return Textos.dinero(oferta.precioUnidad()) + " " + Textos.tendencia(oferta.tendencia());
    }

    /**
     * La mayor cantidad que permite el selector: de 1 a 99 al comprar, de 1 hasta las unidades que se tienen al vender.
     */
    public int cantidadMaxima(boolean comprando, String idArticulo) {
        if (comprando) {
            return esAnimal(idArticulo) ? 1 : COMPRAR_MAXIMO;
        }
        return vender(idArticulo).map(VistaOfertaVender::propio).orElse(0);
    }

    /** El «Total previsto» de vender la cantidad ahora, o el mensaje de rechazo. */
    public String ventaVistaPrevia(String idArticulo, int cantidad) {
        Resultado<Integer> cotizar = juego.cotizar(idArticulo, cantidad);
        return cotizar.isOk() ? "Total previsto: " + Textos.dinero(cotizar.value()) : cotizar.mensaje();
    }

    /** Lo que costaría comprar, como texto. */
    public String compraVistaPrevia(String idArticulo, int cantidad) {
        return comprar(idArticulo).map(oferta -> "Total: " + Textos.dinero(oferta.precio() * cantidad)).orElse("");
    }

    /** Si el jugador puede pagar la compra (el botón de comprar se deshabilita si no, HU-38 · CA-3). */
    public boolean puedeComprar(String idArticulo, int cantidad) {
        return comprar(idArticulo).map(oferta -> cantidad >= 1 && cantidad <= cantidadMaxima(true, idArticulo)
                && oferta.precio() * cantidad <= dinero && !noEspacio(idArticulo)).orElse(false);
    }

    /** La razón por la que una compra está bloqueada, con las palabras de RN-35, o un texto vacío si se puede hacer. */
    public String bloquearCompra(String idArticulo, int cantidad) {
        Optional<VistaOfertaComprar> oferta = comprar(idArticulo);
        if (oferta.isPresent() && cantidad >= 1 && oferta.get().precio() * cantidad > dinero) {
            return Mensajes.of(CodigoError.SIN_FONDOS, oferta.get().precio() * cantidad - dinero);
        }
        if (noEspacio(idArticulo)) {
            return Mensajes.of(CodigoError.SIN_ESPACIO_ANIMALES, Nombres.animalPlural(idArticulo));
        }
        return "";
    }

    /** Compra y responde el mensaje que se muestra; el mercado y el saldo se recargan. */
    public String unidadesComprar(String idArticulo, int cantidad) {
        if (esAnimal(idArticulo)) {
            return animalComprar(idArticulo);
        }
        Resultado<Integer> resultado = juego.comprar(idArticulo, cantidad);
        String name = comprar(idArticulo).map(VistaOfertaComprar::name).orElse(idArticulo);
        refrescar();
        return resultado.isOk() ? "Compraste " + cantidad + " × " + name + " por " + Textos.dinero(resultado.value()) + "."
                : resultado.mensaje();
    }

    /** Vende y responde el mensaje que se muestra; el mercado y el saldo se recargan. */
    public String unidadesVender(String idArticulo, int cantidad) {
        String name = vender(idArticulo).map(VistaOfertaVender::name).orElse(idArticulo);
        Resultado<Integer> resultado = juego.vender(idArticulo, cantidad);
        refrescar();
        return resultado.isOk() ? "Vendiste " + cantidad + " × " + name + " por " + Textos.dinero(resultado.value()) + "."
                : resultado.mensaje();
    }

    private String animalComprar(String animalId) {
        String name = comprar(animalId).map(VistaOfertaComprar::name).orElse(animalId);
        Resultado<VistaEdificio> resultado = juego.animalComprar(animalId);
        refrescar();
        return resultado.isOk() ? "Compraste: " + name + ". Vive en el " + resultado.value().name() + " "
                + (resultado.value().id() + 1) + "." : resultado.mensaje();
    }

    private boolean noEspacio(String id) {
        return animal(id).map(oferta -> oferta.lugaresLibres() <= 0).orElse(false);
    }

    private Optional<VistaOfertaAnimal> animal(String id) {
        return vista.animales().stream().filter(oferta -> oferta.animalId().equals(id)).findFirst();
    }

    private Optional<VistaOfertaVender> vender(String idArticulo) {
        return vista.vende().stream().filter(oferta -> oferta.idArticulo().equals(idArticulo)).findFirst();
    }

    private Optional<VistaOfertaComprar> comprar(String idArticulo) {
        return compra().stream().filter(oferta -> oferta.idArticulo().equals(idArticulo)).findFirst();
    }
}
