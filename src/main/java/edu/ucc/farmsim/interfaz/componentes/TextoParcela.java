package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.aplicacion.dto.VistaEdificio;
import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
import edu.ucc.farmsim.aplicacion.resultado.Nombres;

/**
 * El texto del tooltip de una parcela (HU-07), en español. No tiene Swing, así que se prueba sin pantalla.
 *
 * <p>Examples: {@code Tomate · Brote · Regado: sí · Faltan 5 días}, {@code Tierra de cultivo (libre)},
 * {@code Zanahoria · Marchito}.
 *
 * <p>Stories: HU-07, HU-25, HU-35.
 */
public final class TextoParcela {

    private static final String SEPARADOR = " · ";
    private static final String LIBRE = " (libre)";
    private static final String RESGUARDADO = "Invernadero";

    private TextoParcela() {
                // solo métodos de utilidad estáticos
    }

    /** Describe una parcela tal como la muestra el tooltip. */
    public static String describir(VistaParcela parcela) {
        if (parcela.resguardado()) {
            return parcela.cultivo() == null ? RESGUARDADO + LIBRE : RESGUARDADO + SEPARADOR + cultivoDescribir(parcela);
        }
        if (parcela.cultivo() == null) {
            String terreno = Nombres.terreno(parcela.terreno());
            return "SOIL".equals(parcela.terreno()) ? terreno + LIBRE : terreno;
        }
        return cultivoDescribir(parcela);
    }

    private static String cultivoDescribir(VistaParcela parcela) {
        String cultivo = Nombres.cultivo(parcela.cultivo());
        String etapa = Nombres.etapa(parcela.etapa());
        if ("WITHERED".equals(parcela.etapa())) {
            return cultivo + SEPARADOR + etapa;
        }
        if ("MATURE".equals(parcela.etapa())) {
            return cultivo + SEPARADOR + etapa + SEPARADOR + "Listo para cosechar";
        }
        return cultivo + SEPARADOR + etapa + SEPARADOR + "Regado: " + (parcela.regado() ? "sí" : "no")
                + SEPARADOR + izquierdaDias(parcela.diasAMaduro());
    }

    /**
     * Describe un edificio tal como lo muestra el tooltip, por ejemplo {@code Gallinero 1 · 2/4 animales · 1
     * pendiente}.
     */
    public static String describir(VistaEdificio edificio) {
        StringBuilder texto = new StringBuilder(edificio.name()).append(' ').append(edificio.id() + 1);
        if (edificio.capacidad() > 0) {
            texto.append(SEPARADOR).append(edificio.animales().size()).append('/').append(edificio.capacidad())
                    .append(" animales");
        }
        if (edificio.productosPendientes() > 0) {
            texto.append(SEPARADOR).append(edificio.productosPendientes()).append(" por recolectar");
        }
        return texto.toString();
    }

    private static String izquierdaDias(int dias) {
        return dias == 1 ? "Falta 1 día" : "Faltan " + dias + " días";
    }
}
