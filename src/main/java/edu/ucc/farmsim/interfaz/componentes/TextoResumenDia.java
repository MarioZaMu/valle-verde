package edu.ucc.farmsim.interfaz.componentes;

import edu.ucc.farmsim.aplicacion.dto.VistaResumenDia;
import edu.ucc.farmsim.interfaz.tema.Textos;
import java.util.ArrayList;
import java.util.List;

/**
 * Las líneas del resumen del día, en español y sin Swing (HU-17).
 *
 * <p>Stories: HU-17.
 */
public final class TextoResumenDia {

    private TextoResumenDia() {
                // solo métodos de utilidad estáticos
    }

    /** Las líneas que muestra el diálogo del resumen, de arriba abajo. */
    public static List<String> lineas(VistaResumenDia resumen) {
        List<String> lineas = new ArrayList<>();
        lineas.add(Textos.fecha(resumen.anio(), resumen.estacion(), resumen.dia()));
        lineas.add("Ingresos: " + Textos.dinero(resumen.ingresos()));
        lineas.add("Gastos: " + Textos.dinero(resumen.gastos()));
        lineas.add("Saldo anterior: " + Textos.dinero(resumen.antesSaldo()));
        lineas.add("Saldo nuevo: " + Textos.dinero(resumen.despuesSaldo()));
        lineas.addAll(resumen.luces());
        return lineas;
    }
}
