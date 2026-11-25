package edu.ucc.farmsim.dominio.granja;

import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import edu.ucc.farmsim.dominio.cultivo.Cultivo;
import edu.ucc.farmsim.dominio.cultivo.InstantaneaCultivo;
import edu.ucc.farmsim.dominio.edificio.DirectorEdificio;
import edu.ucc.farmsim.dominio.edificio.Edificio;
import edu.ucc.farmsim.dominio.edificio.InstantaneaEdificio;
import edu.ucc.farmsim.dominio.edificio.TipoEdificio;
import edu.ucc.farmsim.dominio.npc.Npc;
import edu.ucc.farmsim.dominio.tiempo.Clima;
import edu.ucc.farmsim.dominio.tiempo.Estacion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * La grilla de la granja: {@link ReglasJuego#COLS_MAPA} x {@link ReglasJuego#FILAS_MAPA} parcelas con coordenadas {@code
 * (columna, fila)}, donde {@code (0, 0)} es la esquina superior izquierda (RN-04).
 *
 * <p>Stories: HU-01, HU-03, HU-05, HU-12, HU-18, HU-25, HU-28, HU-45, HU-46, HU-50.
 */
public final class MapaGranja {

    private final Parcela[][] parcelas;
    private final List<Edificio> edificios = new ArrayList<>();

    private MapaGranja(Parcela[][] parcelas) {
        this.parcelas = parcelas;
    }

    /**
     * Construye el mapa inicial fijo de una partida nueva: el bloque de tierra, el estanque, los obstáculos, el camino
     * y las casas del valle, y pasto en el resto (RN-04).
     */
    public static MapaGranja inicial() {
        Parcela[][] grilla = new Parcela[ReglasJuego.FILAS_MAPA][ReglasJuego.COLS_MAPA];
        for (int fila = 0; fila < ReglasJuego.FILAS_MAPA; fila++) {
            for (int col = 0; col < ReglasJuego.COLS_MAPA; col++) {
                grilla[fila][col] = new Parcela(terrenoInicial(col, fila));
            }
        }
        return new MapaGranja(grilla);
    }

    private static Terreno terrenoInicial(int col, int fila) {
        if (dentro(col, fila, ReglasJuego.COL_TIERRA, ReglasJuego.FILA_TIERRA, ReglasJuego.ANCHO_TIERRA, ReglasJuego.ALTO_TIERRA)) {
            return Terreno.SOIL;
        }
        if (dentro(col, fila, ReglasJuego.COL_ESTANQUE, ReglasJuego.FILA_ESTANQUE, ReglasJuego.ANCHO_ESTANQUE, ReglasJuego.ALTO_ESTANQUE)) {
            return Terreno.WATER;
        }
        for (Casa casa : Casa.values()) {
            if (dentro(col, fila, casa.col(), casa.fila(), casa.width(), casa.height())) {
                return Terreno.HOUSE;
            }
        }
        for (int[] path : ReglasJuego.RUTAS) {
            if (dentro(col, fila, path[0], path[1], path[2], path[3])) {
                return Terreno.PATH;
            }
        }
        for (int[] obstaculo : ReglasJuego.OBSTACULOS) {
            if (obstaculo[0] == col && obstaculo[1] == fila) {
                return Terreno.OBSTACLE;
            }
        }
        return Terreno.GRASS;
    }

    private static boolean dentro(int col, int fila, int izquierda, int arriba, int width, int height) {
        return col >= izquierda && col < izquierda + width && fila >= arriba && fila < arriba + height;
    }

    /** Número de columnas. */
    public int cols() {
        return ReglasJuego.COLS_MAPA;
    }

    /** Número de filas. */
    public int filas() {
        return ReglasJuego.FILAS_MAPA;
    }

    /** Verdadero si las coordenadas están dentro del mapa. */
    public boolean contains(int col, int fila) {
        return col >= 0 && col < cols() && fila >= 0 && fila < filas();
    }

    /**
     * La parcela en las coordenadas dadas.
     *
     * @throws IllegalArgumentException si las coordenadas están fuera del mapa
     */
    public Parcela parcelaEn(int col, int fila) {
        if (!contains(col, fila)) {
            throw new IllegalArgumentException("outside the map: (" + col + ", " + fila + ")");
        }
        return parcelas[fila][col];
    }

    /**
     * Verdadero si el avatar puede pararse en la parcela (RN-36): está dentro del mapa, es pasto, tierra (con o sin
     * cultivo) o camino, ningún edificio la cubre y ninguna persona del valle está sobre ella (RN-37). El agua, los
     * obstáculos y las casas bloquean el paso.
     */
    public boolean esTransitable(int col, int fila) {
        if (!contains(col, fila)) {
            return false;
        }
        Parcela parcela = parcelas[fila][col];
        Terreno suelo = parcela.terreno();
        return (suelo == Terreno.GRASS || suelo == Terreno.SOIL || suelo == Terreno.PATH) && parcela.edificio() == null
                && Npc.at(col, fila) == null;
    }

    /**
     * La parcela transitable más cercana a la dada, contando pasos por columnas y filas; los empates van a la fila de
     * arriba y luego a la columna de la izquierda. La propia parcela dada si es transitable.
     *
     * @return {columna, fila}
     * @throws IllegalStateException si ninguna parcela del mapa es transitable
     */
    public int[] cercanoTransitable(int col, int fila) {
        for (int distancia = 0; distancia <= cols() + filas(); distancia++) {
            for (int r = fila - distancia; r <= fila + distancia; r++) {
                int resto = distancia - Math.abs(r - fila);
                for (int c : resto == 0 ? new int[] {col} : new int[] {col - resto, col + resto}) {
                    if (esTransitable(c, r)) {
                        return new int[] {c, r};
                    }
                }
            }
        }
        throw new IllegalStateException("no plot of the map can be walked on");
    }

    /** Los edificios en el orden en que se construyeron; el índice de un edificio es su id. */
    public List<Edificio> edificios() {
        return Collections.unmodifiableList(edificios);
    }

    /** Cuántos edificios del tipo se han construido. */
    public int contarEdificios(TipoEdificio tipo) {
        return (int) edificios.stream().filter(b -> b.tipo() == tipo).count();
    }

    /** El edificio que cubre una parcela, o {@code null}. */
    public Edificio edificioEn(int col, int fila) {
        return contains(col, fila) ? parcelas[fila][col].edificio() : null;
    }

    /**
     * Verdadero si cabe un edificio del tipo con su parcela superior izquierda en el lugar dado: todas las parcelas del
     * área están dentro del mapa, son pasto y están libres (RN-21 punto 1).
     */
    public boolean puedeColocar(TipoEdificio tipo, int col, int fila) {
        for (int r = fila; r < fila + tipo.height(); r++) {
            for (int c = col; c < col + tipo.width(); c++) {
                if (!contains(c, r) || parcelas[r][c].terreno() != Terreno.GRASS || parcelas[r][c].edificio() != null) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Construye un edificio y marca sus parcelas como ocupadas.
     *
     * @throws IllegalStateException si el área no es pasto libre dentro del mapa
     */
    public Edificio colocar(TipoEdificio tipo, int col, int fila) {
        if (!puedeColocar(tipo, col, fila)) {
            throw new IllegalStateException("Cannot build " + tipo + " at (" + col + ", " + fila + ")");
        }
        return adjuntar(DirectorEdificio.armar(tipo, edificios.size(), col, fila));
    }

    private Edificio adjuntar(Edificio edificio) {
        for (int r = edificio.fila(); r < edificio.fila() + edificio.height(); r++) {
            for (int c = edificio.col(); c < edificio.col() + edificio.width(); c++) {
                parcelas[r][c].edificioFijar(edificio);
            }
        }
        edificios.add(edificio);
        return edificio;
    }

    /** Número de parcelas con el terreno dado. */
    public int count(Terreno terreno) {
        int total = 0;
        for (Parcela[] linea : parcelas) {
            for (Parcela parcela : linea) {
                if (parcela.terreno() == terreno) {
                    total++;
                }
            }
        }
        return total;
    }

    /**
     * Resuelve el final del día de cada cultivo del mapa (paso 2 de RN-03): crecimiento, día seco o marchitamiento. Una
     * sequía reduce la tolerancia a los días secos (RN-09).
     *
     * @param climaHoy clima del día que termina
     * @return número de cultivos que se marchitaron ahora
     */
    public int cultivosCrecer(Clima climaHoy) {
        int toleranciaAbrir = climaHoy == Clima.DROUGHT
                ? ReglasJuego.DIAS_SECOS_A_MARCHITAR_EN_SEQUIA
                : ReglasJuego.DIAS_SECOS_A_MARCHITAR;
        int marchito = 0;
        for (Parcela[] linea : parcelas) {
            for (Parcela parcela : linea) {
                Cultivo cultivo = parcela.cultivo();
                if (cultivo != null) {
                    boolean fueVivos = cultivo.esVivo();
                    cultivo.terminarDeDia(parcela.esResguardado() ? ReglasJuego.DIAS_SECOS_A_MARCHITAR_EN_INVERNADERO : toleranciaAbrir);
                    if (fueVivos && !cultivo.esVivo()) {
                        marchito++;
                    }
                }
            }
        }
        return marchito;
    }

    /**
     * La lluvia riega todos los cultivos vivos a la intemperie (paso 1 de RN-03); los que están dentro de un
     * invernadero no reciben lluvia (RN-22).
     */
    public void lluviaOnCultivos() {
        for (Parcela[] linea : parcelas) {
            for (Parcela parcela : linea) {
                if (parcela.cultivo() != null && !parcela.esResguardado()) {
                    parcela.cultivo().agua();
                }
            }
        }
    }

    /**
     * Una helada marchita todos los cultivos vivos a la intemperie, sea cual sea su etapa (RN-09-b).
     *
     * @return cuántos cultivos se marchitaron ahora
     */
    public int cultivosHelada() {
        int marchito = 0;
        for (Parcela[] linea : parcelas) {
            for (Parcela parcela : linea) {
                if (parcela.cultivo() != null && !parcela.esResguardado() && parcela.cultivo().marchitarPorHelada()) {
                    marchito++;
                }
            }
        }
        return marchito;
    }

    /**
     * Marchita todos los cultivos vivos que no son válidos en la estación dada (RN-09-c, paso 5 de RN-03).
     *
     * @return número de cultivos que se marchitaron ahora
     */
    public int fueraMarchitarDeEstacion(Estacion estacion) {
        int marchito = 0;
        for (Parcela[] linea : parcelas) {
            for (Parcela parcela : linea) {
                if (parcela.cultivo() != null && !parcela.esResguardado() && parcela.cultivo().marchitarSiFueraDeEstacion(estacion)) {
                    marchito++;
                }
            }
        }
        return marchito;
    }

    /** Copia de los cultivos de cada parcela en orden de fila ({@code null} donde no hay), para el memento. */
    public InstantaneaMapaGranja instantanea() {
        List<InstantaneaCultivo> cultivos = new ArrayList<>();
        for (Parcela[] linea : parcelas) {
            for (Parcela parcela : linea) {
                cultivos.add(parcela.cultivo() == null ? null : parcela.cultivo().instantanea());
            }
        }
        return new InstantaneaMapaGranja(Collections.unmodifiableList(cultivos),
                edificios.stream().map(Edificio::instantanea).toList());
    }

    /** Devuelve cada cultivo y cada edificio a como estaban cuando se tomó la instantánea. */
    public void restaurarDesde(InstantaneaMapaGranja instantanea) {
        int indice = 0;
        for (Parcela[] linea : parcelas) {
            for (Parcela parcela : linea) {
                InstantaneaCultivo archivado = instantanea.cultivos().get(indice++);
                parcela.cultivoRestaurar(archivado == null ? null : Cultivo.desdeInstantanea(archivado));
                parcela.edificioFijar(null);
            }
        }
        edificios.clear();
        for (InstantaneaEdificio archivado : instantanea.edificios()) {
            adjuntar(Edificio.desdeInstantanea(archivado));
        }
    }
}

