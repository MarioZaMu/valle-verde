package edu.ucc.farmsim.aplicacion.dto;

/**
 * Una fila de la pestaña «Vender»: un producto con cuántas unidades tiene el jugador, el precio de la siguiente unidad
 * y su tendencia.
 *
 * <p>Stories: HU-15, HU-16, HU-38.
 *
 * @param idArticulo    id del objeto, por ejemplo {@code CARROT}
 * @param name      nombre en español
 * @param propio     unidades en el inventario
 * @param precioUnidad precio en m de la siguiente unidad
 * @param tendencia     {@code UP}, {@code DOWN} or {@code SAME} (RN-19)
 */
public record VistaOfertaVender(String idArticulo, String name, int propio, int precioUnidad, String tendencia) { }
