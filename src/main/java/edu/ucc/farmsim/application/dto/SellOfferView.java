package edu.ucc.farmsim.application.dto;

/**
 * Una fila de la pestaña «Vender»: un producto con cuántas unidades tiene el jugador, el precio de la siguiente unidad
 * y su tendencia.
 *
 * <p>Stories: HU-15, HU-16, HU-38.
 *
 * @param itemId    id del objeto, por ejemplo {@code CARROT}
 * @param name      nombre en español
 * @param owned     unidades en el inventario
 * @param unitPrice precio en m de la siguiente unidad
 * @param trend     {@code UP}, {@code DOWN} or {@code SAME} (RN-19)
 */
public record SellOfferView(String itemId, String name, int owned, int unitPrice, String trend) { }
