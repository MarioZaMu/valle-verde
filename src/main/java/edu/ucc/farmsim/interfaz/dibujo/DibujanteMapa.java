package edu.ucc.farmsim.interfaz.dibujo;

import edu.ucc.farmsim.aplicacion.dto.VistaAnimal;
import edu.ucc.farmsim.aplicacion.dto.VistaAvatar;
import edu.ucc.farmsim.aplicacion.dto.VistaCasa;
import edu.ucc.farmsim.aplicacion.dto.VistaEdificio;
import edu.ucc.farmsim.aplicacion.dto.VistaMapa;
import edu.ucc.farmsim.aplicacion.dto.VistaNpc;
import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
import edu.ucc.farmsim.interfaz.recursos.GestorRecurso;
import edu.ucc.farmsim.interfaz.tema.Paleta;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Dibuja el mapa de la granja con Java2D, de una de dos maneras.
 *
 * <p>Con el paquete de pixel art (HU-44, HU-48) la vista es un trozo de campo visto desde arriba a tres cuartos: un
 * suelo continuo donde la tierra, el camino y el agua se funden con el pasto, bosques alrededor del valle, y árboles,
 * casas, edificios, cultivos y animales que se paran sobre su baldosa, se elevan sobre ella y se dibujan de atrás hacia
 * adelante, cada uno con su sombra. La granja queda al oeste y la aldea con sus casas al este (HU-50). Lo que tapa un
 * cultivo o la parcela bajo el cursor se vuelve translúcido. Con la vida ambiental activada, el agua ondea, los árboles
 * se mecen, los animales deambulan y nubes, mariposas u hojas cruzan los campos. El avatar está entre ellos y camina de
 * parcela en parcela con tres cuadros (HU-45), y las personas del valle están sobre sus parcelas con un globo que
 * muestra lo que pide su encargo mientras no está hecho (HU-46, HU-47). Las personas miden una parcela de ancho y dos
 * de alto: sus pies están en su parcela y su cabeza se eleva a la parcela de arriba (HU-50).
 *
 * <p>Sin el paquete, cada terreno y cada etapa de cultivo se dibuja con formas vectoriales, de modo que el juego no
 * necesita ninguna imagen (HU-42); un solo sprite que falte también recurre a su forma vectorial. Con unos {@link
 * EfectosVisuales} pinta la paleta de la estación, el clima, el tinte de noche y las animaciones (HU-40, HU-41). Funciona
 * sobre cualquier {@link Graphics2D}, incluido el de una imagen, así que se prueba sin pantalla.
 *
 * <p>Stories: HU-18, HU-19, HU-25, HU-35, HU-40, HU-41, HU-42, HU-44, HU-45, HU-46, HU-47, HU-48, HU-50.
 */
public final class DibujanteMapa {

    private static final int T = GeometriaMapa.BALDOSA;
    private static final float TALLO = 0.30f;
    private static final float LEAF_W = 0.22f;
    private static final float LEAF_H = 0.12f;
    private static final float FRUTA = 0.16f;
    private static final int ALFA_NOCHE_MAXIMA = 150;
    private static final float ALFA_DETRAS = 0.5f;
    private static final int MS_MARCO_AGUA = 450;
    private static final double TRONCO_ARBOL_DESDE = 44.0 / 64.0;
    private static final Color NOCHE = new Color(10, 20, 60);
    private static final Color SOL = new Color(0xFFC21A);
    private static final Color COPO = new Color(0xFFFFFF);
    private static final Color RAIN = new Color(0xBFD9FF);
    private static final Color SOMBRA = new Color(0, 0, 0, 56);
    private static final Color CURSOR = new Color(0xFFFFFF);
    private static final Color BORDE_CURSOR = new Color(0x3B2412);
    private static final Color RELLENAR_CURSOR = new Color(255, 255, 255, 48);
    private static final Color GLOBO = new Color(0xFFF8E7);
    private static final Color CAMISA_AVATAR = new Color(0x3F7FBF);
    private static final Color PIEL_AVATAR = new Color(0xF2C79B);
    private static final Color SOMBRERO_AVATAR = new Color(0xC8963E);
    private static final Color OJO_AVATAR = new Color(0x2A1A10);
    private static final Color NPC_MARTA = new Color(0xC0504D);
    private static final Color NPC_TOMAS = new Color(0x6B8E23);
    private static final Color NPC_LUCIA = new Color(0xF2F2F2);
    private static final Color NPC_INES = new Color(0x8E5BB5);
    private static final Color PANTALONES = new Color(0x4A3B2A);
    private static final Color PATH = new Color(0xC9A66B);
    private static final Color PARED = new Color(0xE8D7B0);
    private static final Color TECHO = new Color(0xA0442E);
    private static final Color PUERTA = new Color(0x6B4226);
    private static final int COPIAS_SUELO = 4;
    /** Baldosas de bosque pintadas alrededor del valle; más solo cuando la ventana muestra más que eso fuera de él. */
    private static final int BORDE_SUELO = 8;
    /** Baldosas más allá del borde de la ventana, para los árboles que lo sobrepasan. */
    private static final int DERRAME_SUELO = 2;

    private final GestorRecurso recursos;
    private final EscaladorPixel escalador = new EscaladorPixel();
    private final EscaladorPixel escaladorSuelo = new EscaladorPixel(COPIAS_SUELO);
    private final CapaSuelo suelo;

    /** Algo que está en el mapa, dibujado en orden de sus pies (cuanto más abajo, más tarde). */
    private record Articulo(double profundidad, int orden, Runnable paint) {
    }

    /** Crea un renderizador que busca sprites en la carpeta {@code recursos} del classpath. */
    public DibujanteMapa() {
        this(GestorRecurso.compartido());
    }

    /**
     * Crea un renderizador.
     *
     * @param recursos de donde vienen los sprites
     */
    public DibujanteMapa(GestorRecurso recursos) {
        this.recursos = recursos;
        this.suelo = new CapaSuelo(recursos);
    }

    /**
     * Pinta todo el mapa en el área dada, sin efectos.
     *
     * @param g      dónde pintar
     * @param map    el mapa que se dibuja
     * @param width  ancho del área
     * @param height alto del área
     * @param sobrevolar  parcela bajo el cursor, o {@code null}
     */
    public void renderizar(Graphics2D g, VistaMapa map, int width, int height, PosCelda sobrevolar) {
        renderizar(g, map, width, height, sobrevolar, null);
    }

    /**
     * Pinta todo el mapa en el área dada.
     *
     * @param g       dónde pintar
     * @param map     el mapa que se dibuja
     * @param width   ancho del área
     * @param height  alto del área
     * @param sobrevolar   parcela bajo el cursor, o {@code null}
     * @param efectos la estación, el clima y las animaciones que se muestran, o {@code null} para el aspecto simple
     */
    public void renderizar(Graphics2D g, VistaMapa map, int width, int height, PosCelda sobrevolar, EfectosVisuales efectos) {
        renderizar(g, map, GeometriaMapa.ajustar(map.cols(), map.filas(), width, height), width, height, sobrevolar, efectos);
    }

    /**
     * Pinta todo el mapa con la colocación de parcelas dada; la tierra alrededor llena el resto.
     *
     * @param g        dónde pintar
     * @param map      el mapa que se dibuja
     * @param geometria dónde va cada parcela
     * @param width    ancho del área
     * @param height   alto del área
     * @param sobrevolar    parcela bajo el cursor, o {@code null}
     * @param efectos  la estación, el clima y las animaciones que se muestran, o {@code null} para el aspecto simple
     */
    public void renderizar(Graphics2D g, VistaMapa map, GeometriaMapa geometria, int width, int height, PosCelda sobrevolar,
                       EfectosVisuales efectos) {
        EfectosVisuales fx = efectos != null && efectos.isEnabled() ? efectos : null;
        boolean ambiente = fx != null && fx.esAmbiente();
        long tiempo = fx == null ? 0 : fx.tiempo();
        String estacion = fx == null ? "SPRING" : fx.estacion();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (suelo.disponible(estacion)) {
            artePixelPintar(g, map, geometria, width, height, sobrevolar, fx, estacion, ambiente, tiempo);
        } else {
            vectorPintar(g, map, geometria, width, height, fx, estacion);
        }
        if (sobrevolar != null && sobrevolar.col() >= 0 && sobrevolar.fila() >= 0 && sobrevolar.col() < map.cols() && sobrevolar.fila() < map.filas()) {
            cursorDibujar(g, geometria.limitesCelda(sobrevolar.col(), sobrevolar.fila()), ambiente, tiempo);
        }
        if (fx != null) {
            ambienteDibujar(g, fx, geometria, width, height, ambiente);
        }
    }

    /** Cuántas veces se pintó el suelo; una cámara en movimiento lo reutiliza (HU-50 · CA-5). */
    public int pintaSuelo() {
        return suelo.pinta();
    }

    /** Baldosas de suelo que se pintan más allá de un lado del valle, que muestra {@code afuera} píxeles más allá. */
    private static int borde(int afuera, int celda) {
        return Math.max(BORDE_SUELO, divTecho(Math.max(0, afuera), celda) + DERRAME_SUELO);
    }

        // ------------------------------------------------------------------ pixel art

    private void artePixelPintar(Graphics2D g, VistaMapa map, GeometriaMapa geo, int width, int height, PosCelda sobrevolar,
                               EfectosVisuales fx, String estacion, boolean ambiente, long tiempo) {
        int s = geo.tamanioCelda();
                // El suelo de todo el valle más un borde de bosque, sea lo que sea que muestre la cámara: caminar solo lo
        // desliza. Unos límites que seguían a la cámara lo pintaban y escalaban otra vez en cada fila y hacían que la
        // caminata diera tirones.
        int col0 = -borde(geo.originX(), s);
        int row0 = -borde(geo.originY(), s);
        int col1 = map.cols() + borde(width - geo.originX() - map.cols() * s, s);
        int row1 = map.filas() + borde(height - geo.originY() - map.filas() * s, s);
        int marco = ambiente ? (int) ((tiempo / MS_MARCO_AGUA) % CapaSuelo.MARCOS_AGUA) : 0;
        BufferedImage arte = suelo.imagen(map, estacion, marco, col0, row0, col1, row1);
        escaladorSuelo.draw(g, arte, geo.originX() + col0 * s, geo.originY() + row0 * s, (col1 - col0) * s, (row1 - row0) * s);

        Set<Long> oculto = new HashSet<>();
        for (VistaParcela parcela : map.parcelas()) {
            if (parcela.cultivo() != null) {
                oculto.add(clave(parcela.col(), parcela.fila()));
            }
        }
        if (sobrevolar != null) {
            oculto.add(clave(sobrevolar.col(), sobrevolar.fila()));
        }
        if (map.avatar() != null) {
            oculto.add(clave(map.avatar().col(), map.avatar().fila()));
        }
        for (VistaNpc npc : npcs(map)) {
            oculto.add(clave(npc.col(), npc.fila()));
        }
        List<Rectangle> sombras = new ArrayList<>();
        List<Articulo> articulos = new ArrayList<>();
        List<Runnable> encima = new ArrayList<>();
        alrededores(g, map, geo, estacion, ambiente, tiempo, col0, row0, col1, row1, sombras, articulos);
        for (VistaParcela parcela : map.parcelas()) {
            Rectangle celda = geo.limitesCelda(parcela.col(), parcela.fila());
            if ("OBSTACLE".equals(parcela.terreno())) {
                obstaculo(g, parcela.col(), parcela.fila(), celda, estacion, ambiente, tiempo, oculto, sombras, articulos);
            }
            if (parcela.cultivo() != null) {
                double profundidad = celda.y + celda.height - (parcela.resguardado() ? 0.5 : 0.25);
                articulos.add(new Articulo(profundidad, articulos.size(), () -> arteCultivoDibujar(g, parcela, celda, fx)));
            }
        }
        for (VistaEdificio edificio : map.edificios()) {
            edificio(g, edificio, geo, fx, ambiente, tiempo, oculto, sombras, articulos, encima);
        }
        for (VistaCasa casa : casas(map)) {
            casa(g, casa, geo, oculto, sombras, articulos);
        }
        if (map.avatar() != null) {
            avatar(g, map.avatar(), geo, fx, sombras, articulos);
        }
        for (VistaNpc npc : npcs(map)) {
            npc(g, npc, geo, ambiente, tiempo, sombras, articulos, encima);
        }
        Composite archivado = g.getComposite();
        g.setColor(SOMBRA);
        for (Rectangle sombra : sombras) {
            g.fillOval(sombra.x, sombra.y, sombra.width, sombra.height);
        }
        g.setComposite(archivado);
        articulos.sort(Comparator.comparingDouble(Articulo::profundidad).thenComparingInt(Articulo::orden));
        for (Articulo articulo : articulos) {
            articulo.paint().run();
        }
        for (Runnable globo : encima) {
            globo.run();
        }
    }

    /** Los bosques más allá de los bordes del valle, sobre las baldosas fuera del mapa. */
    private void alrededores(Graphics2D g, VistaMapa map, GeometriaMapa geo, String estacion, boolean ambiente, long tiempo,
                              int col0, int row0, int col1, int row1, List<Rectangle> sombras, List<Articulo> articulos) {
        int s = geo.tamanioCelda();
        for (int fila = row0; fila < row1; fila++) {
            for (int col = col0; col < col1; col++) {
                int dx = col < 0 ? -col : Math.max(0, col - map.cols() + 1);
                int dy = fila < 0 ? -fila : Math.max(0, fila - map.filas() + 1);
                int distancia = Math.max(dx, dy);
                if (distancia == 0) {
                    continue;
                }
                Rectangle celda = geo.limitesCelda(col, fila);
                int h = hash(col, fila) % 100;
                if (h < 42) {
                    String categoria = h % 3 == 0 ? "PINE" : "TREE";
                    arbol(g, "terrain/" + categoria + "_" + estacion + ".png", col, fila, celda, ambiente, tiempo, false, sombras, articulos);
                } else if (h < 52) {
                    recursos.sprite("terrain/BUSH_" + estacion + ".png").ifPresent(img -> {
                        sombras.add(new Rectangle(celda.x + s / 8, celda.y + s * 3 / 4, s * 3 / 4, s / 4));
                        articulos.add(new Articulo(celda.y + celda.height - 1, articulos.size(), () -> escalador.draw(g, img, celda.x, celda.y, s, s)));
                    });
                }
            }
        }
    }

    private void obstaculo(Graphics2D g, int col, int fila, Rectangle celda, String estacion, boolean ambiente, long tiempo,
                          Set<Long> oculto, List<Rectangle> sombras, List<Articulo> articulos) {
        int h = hash(col, fila);
        if (h % 4 == 0) {
            Optional<BufferedImage> roca = recursos.sprite("terrain/OBSTACLE.png");
            if (roca.isPresent()) {
                sombras.add(new Rectangle(celda.x + celda.width / 10, celda.y + celda.height * 3 / 4, celda.width * 4 / 5, celda.height / 4));
                articulos.add(new Articulo(celda.y + celda.height - 1, articulos.size(),
                        () -> escalador.draw(g, roca.get(), celda.x, celda.y, celda.width, celda.height)));
                return;
            }
        }
        String categoria = h % 3 == 0 ? "PINE" : "TREE";
        boolean detras = oculto.contains(clave(col, fila - 1)) || oculto.contains(clave(col - 1, fila - 1))
                || oculto.contains(clave(col + 1, fila - 1));
        if (!arbol(g, "terrain/" + categoria + "_" + estacion + ".png", col, fila, celda, ambiente, tiempo, detras, sombras, articulos)) {
            articulos.add(new Articulo(celda.y + celda.height - 1, articulos.size(), () -> vectorObstaculoDibujar(g, celda)));
        }
    }

    private boolean arbol(Graphics2D g, String name, int col, int fila, Rectangle celda, boolean ambiente, long tiempo,
                         boolean detras, List<Rectangle> sombras, List<Articulo> articulos) {
        Optional<BufferedImage> sprite = recursos.sprite(name);
        if (sprite.isEmpty()) {
            return false;
        }
        int s = celda.width;
        sombras.add(new Rectangle(celda.x + s / 20, celda.y + s * 3 / 4, s * 9 / 10, s / 3));
        double balanceo = ambiente ? Math.sin(tiempo / 1100.0 + col * 0.9 + fila * 0.5) * s / 28.0 : 0;
        articulos.add(new Articulo(celda.y + celda.height - 1, articulos.size(), () -> dividirDibujar(g, sprite.get(),
                celda.x - s / 2, celda.y - s, 2 * s, 2 * s, TRONCO_ARBOL_DESDE, (int) Math.round(balanceo), detras ? ALFA_DETRAS : 1f)));
        return true;
    }

    /**
     * Dibuja un sprite alto cuya parte superior (la copa) puede inclinarse hacia un lado mientras la parte inferior
     * queda quieta.
     */
    private void dividirDibujar(Graphics2D g, BufferedImage img, int x, int y, int w, int h, double split, int inclinar, float alfa) {
        Composite archivado = g.getComposite();
        if (alfa < 1f) {
            g.setComposite(AlphaComposite.SrcOver.derive(alfa));
        }
        if (inclinar == 0) {
            escalador.draw(g, img, x, y, w, h);
        } else {
            double dispositivo = EscaladorPixel.escalaDispositivo(g);
            BufferedImage listo = escalador.escalado(img, (int) Math.round(w * dispositivo), (int) Math.round(h * dispositivo));
            int cortar = (int) Math.round(h * split);
            int dispositivoCortar = (int) Math.round(listo.getHeight() * split);
            g.drawImage(listo, x + inclinar, y, x + inclinar + w, y + cortar, 0, 0, listo.getWidth(), dispositivoCortar, null);
            g.drawImage(listo, x, y + cortar, x + w, y + h, 0, dispositivoCortar, listo.getWidth(), listo.getHeight(), null);
        }
        g.setComposite(archivado);
    }

    private void edificio(Graphics2D g, VistaEdificio edificio, GeometriaMapa geo, EfectosVisuales fx, boolean ambiente,
                          long tiempo, Set<Long> oculto, List<Rectangle> sombras, List<Articulo> articulos, List<Runnable> encima) {
        Rectangle pie = geo.limitesCelda(edificio.col(), edificio.fila())
                .union(geo.limitesCelda(edificio.col() + edificio.width() - 1, edificio.fila() + edificio.height() - 1));
        int s = geo.tamanioCelda();
        int inferior = pie.y + pie.height;
        Optional<BufferedImage> hoja = recursos.sprite("buildings/" + edificio.tipo() + ".png");
        if (hoja.isEmpty()) {
            articulos.add(new Articulo(inferior, articulos.size(), () -> edificioDibujar(g, edificio, geo, fx)));
            return;
        }
        BufferedImage img = hoja.get();
        int drawH = img.getHeight() * s / T;
        int drawW = img.getWidth() * s / T;
        int arriba = inferior - drawH;
        boolean detras = false;
        for (int col = edificio.col(); col < edificio.col() + edificio.width(); col++) {
            detras |= oculto.contains(clave(col, edificio.fila() - 1));
        }
        float alfa = detras ? ALFA_DETRAS : 1f;
        sombras.add(new Rectangle(pie.x - s / 6, inferior - s / 3, pie.width + s / 3, s / 2));
        articulos.add(new Articulo(inferior, articulos.size(), () -> {
            Composite archivado = g.getComposite();
            if (alfa < 1f) {
                g.setComposite(AlphaComposite.SrcOver.derive(alfa));
            }
            escalador.draw(g, img, pie.x, arriba, drawW, drawH);
            g.setComposite(archivado);
        }));
        int ranuras = Math.max(1, edificio.capacidad());
        for (VistaAnimal animal : edificio.animales()) {
            int marco = fx == null ? 0 : fx.marcoAnimal(animal.indice());
            int carril = pie.width / ranuras;
            double fase = tiempo / 1700.0 + animal.indice() * 1.9;
            int vagar = ambiente ? (int) Math.round(Math.sin(fase) * Math.max(0, carril - s) / 2.0) : 0;
            boolean izquierda = ambiente && Math.cos(fase) < 0;
            int x = pie.x + animal.indice() * carril + (carril - s) / 2 + vagar;
            int y = inferior - s + s / 16;
            sombras.add(new Rectangle(x + s / 6, inferior - s / 6, s * 2 / 3, s / 5));
            articulos.add(new Articulo(inferior + 1, articulos.size(), () -> {
                Optional<BufferedImage> sprite = recursos.sprite("animals/" + animal.tipo() + "_" + marco + ".png");
                if (sprite.isPresent()) {
                    escalador.draw(g, sprite.get(), x, y, izquierda ? -s : s, s);
                } else {
                    vectorAnimalDibujar(g, animal, x, y, s, marco);
                }
            }));
            String necesitar = "SICK".equals(animal.condicion()) ? "items/MEDICINE.png"
                    : "HUNGRY".equals(animal.condicion()) ? "items/HAY.png" : null;
            if (necesitar != null) {
                Color respaldo = "SICK".equals(animal.condicion()) ? Paleta.ANIMAL_ENFERMO : Paleta.ANIMAL_HAMBRIENTO;
                encima.add(() -> globo(g, x + s / 2, y + s / 8, Math.max(12, s / 2), necesitar, respaldo, 0));
            }
        }
        if (edificio.productosPendientes() > 0) {
            String producto = "BARN".equals(edificio.tipo()) ? "items/MILK.png" : "items/EGG.png";
            int vaiven = ambiente ? (int) Math.round(Math.sin(tiempo / 300.0) * s / 12.0) : 0;
            encima.add(() -> globo(g, pie.x + pie.width - s / 2, arriba + s / 2, Math.max(14, s * 3 / 5), producto,
                    Paleta.PENDIENTE, vaiven));
        }
    }

    /** Una casa del valle: su sprite se levanta sobre sus parcelas y su techo se eleva una parcela sobre ellas. */
    private void casa(Graphics2D g, VistaCasa casa, GeometriaMapa geo, Set<Long> oculto, List<Rectangle> sombras,
                       List<Articulo> articulos) {
        Rectangle pie = huella(geo, casa);
        int s = geo.tamanioCelda();
        int inferior = pie.y + pie.height;
        Optional<BufferedImage> sprite = recursos.sprite(spriteCasa(casa));
        boolean detras = false;
        for (int col = casa.col(); col < casa.col() + casa.width(); col++) {
            detras |= oculto.contains(clave(col, casa.fila() - 1));
        }
        float alfa = detras ? ALFA_DETRAS : 1f;
        sombras.add(new Rectangle(pie.x - s / 6, inferior - s / 3, pie.width + s / 3, s / 2));
        articulos.add(new Articulo(inferior, articulos.size(), () -> {
            if (sprite.isEmpty()) {
                vectorCasaDibujar(g, pie, s);
                return;
            }
            BufferedImage img = sprite.get();
            int drawW = img.getWidth() * s / T;
            int drawH = img.getHeight() * s / T;
            Composite archivado = g.getComposite();
            if (alfa < 1f) {
                g.setComposite(AlphaComposite.SrcOver.derive(alfa));
            }
            escalador.draw(g, img, pie.x, inferior - drawH, drawW, drawH);
            g.setComposite(archivado);
        }));
    }

    /** El sprite de una casa del valle. */
    static String spriteCasa(VistaCasa casa) {
        return "village/" + casa.id() + ".png";
    }

    private static Rectangle huella(GeometriaMapa geo, VistaCasa casa) {
        return geo.limitesCelda(casa.col(), casa.fila())
                .union(geo.limitesCelda(casa.col() + casa.width() - 1, casa.fila() + casa.height() - 1));
    }

    /** Una casa sin sprites: paredes claras con una puerta y un techo rojo sobre su parcela superior. */
    private static void vectorCasaDibujar(Graphics2D g, Rectangle pie, int s) {
        int roofH = Math.max(2, pie.height / 2);
        g.setColor(PARED);
        g.fillRect(pie.x, pie.y + roofH, pie.width, pie.height - roofH);
        g.setColor(TECHO);
        g.fillPolygon(new int[] {pie.x - s / 8, pie.x + pie.width + s / 8, pie.x + pie.width - s / 3, pie.x + s / 3},
                new int[] {pie.y + roofH, pie.y + roofH, pie.y, pie.y}, 4);
        g.setColor(PUERTA);
        int doorW = Math.max(2, s / 2);
        g.fillRect(pie.x + (pie.width - doorW) / 2, pie.y + pie.height - s * 3 / 4, doorW, s * 3 / 4);
    }

    /** Un globo de diálogo con un objeto dentro, para lo que necesita un animal o lo que espera ser recogido. */
    private void globo(Graphics2D g, int cx, int inferior, int size, String icono, Color respaldo, int vaiven) {
        int x = cx - size / 2;
        int y = inferior - size - size / 4 + vaiven;
        int arco = Math.max(4, size / 3);
        g.setColor(BORDE_CURSOR);
        g.fillRoundRect(x - 2, y - 2, size + 4, size + 4, arco + 2, arco + 2);
        g.fillPolygon(new int[] {cx - size / 6 - 2, cx + size / 6 + 2, cx}, new int[] {y + size, y + size, y + size + size / 4 + 2}, 3);
        g.setColor(GLOBO);
        g.fillRoundRect(x, y, size, size, arco, arco);
        g.fillPolygon(new int[] {cx - size / 6, cx + size / 6, cx}, new int[] {y + size - 1, y + size - 1, y + size + size / 4}, 3);
        Optional<BufferedImage> sprite = recursos.sprite(icono);
        int margen = Math.max(1, size / 10);
        if (sprite.isPresent()) {
            escalador.draw(g, sprite.get(), x + margen, y + margen, size - 2 * margen, size - 2 * margen);
        } else {
            g.setColor(respaldo);
            g.fillOval(x + margen * 2, y + margen * 2, size - 4 * margen, size - 4 * margen);
        }
    }

    private void arteCultivoDibujar(Graphics2D g, VistaParcela parcela, Rectangle c, EfectosVisuales fx) {
        float scale = fx == null ? 1f : fx.escalaCultivo(parcela.col(), parcela.fila());
        Optional<BufferedImage> sprite = recursos.sprite("crops/" + parcela.cultivo() + "_" + parcela.etapa() + ".png");
        if (sprite.isEmpty()) {
            cultivoDibujar(g, parcela, c, fx);
            return;
        }
        int size = Math.max(1, Math.round(c.width * scale));
        escalador.draw(g, sprite.get(), c.x + (c.width - size) / 2, c.y + c.height - size, size, size);
    }

    /** Corchetes de píxeles en las esquinas de la parcela bajo el cursor, sobre un velo claro; respiran lentamente. */
    private static void cursorDibujar(Graphics2D g, Rectangle celda, boolean ambiente, long tiempo) {
        int pulso = ambiente ? (int) Math.round((Math.sin(tiempo / 220.0) + 1) * celda.width / 48.0) : 0;
        int x = celda.x - pulso;
        int y = celda.y - pulso;
        int w = celda.width + 2 * pulso;
        int h = celda.height + 2 * pulso;
        g.setColor(RELLENAR_CURSOR);
        g.fillRect(celda.x, celda.y, celda.width, celda.height);
        int len = Math.max(4, celda.width / 4);
        int t = Math.max(2, celda.width / 16);
        for (int pasar = 0; pasar < 2; pasar++) {
            int o = pasar == 0 ? 1 : 0;
            g.setColor(pasar == 0 ? BORDE_CURSOR : CURSOR);
            int[][] corners = {{x, y, 1, 1}, {x + w, y, -1, 1}, {x, y + h, 1, -1}, {x + w, y + h, -1, -1}};
            for (int[] k : corners) {
                int cx = k[0];
                int cy = k[1];
                int hx = k[2] > 0 ? cx : cx - len;
                int vy = k[3] > 0 ? cy : cy - len;
                int tx = k[2] > 0 ? cx : cx - t;
                int ty = k[3] > 0 ? cy : cy - t;
                g.fillRect(hx - o, ty - o, len + 2 * o, t + 2 * o);
                g.fillRect(tx - o, vy - o, t + 2 * o, len + 2 * o);
            }
        }
    }

    private static long clave(int col, int fila) {
        return ((long) fila << 32) ^ (col & 0xFFFFFFFFL);
    }

    private static int hash(int col, int fila) {
        int h = col * 374_761_393 + fila * 668_265_263;
        h = (h ^ (h >>> 13)) * 1_274_126_177;
        return (h ^ (h >>> 16)) & 0x7FFFFFFF;
    }

    private static int divTecho(int a, int b) {
        return a <= 0 ? 0 : (a + b - 1) / b;
    }

        // ------------------------------------------------------------------ respaldo vectorial

    private void vectorPintar(Graphics2D g, VistaMapa map, GeometriaMapa geometria, int width, int height, EfectosVisuales fx,
                             String estacion) {
        g.setColor(Paleta.FONDO);
        g.fillRect(0, 0, width, height);
        Color hierba = fx == null ? Paleta.GRASS : PaletaEstacion.hierba(fx.estacion());
        for (VistaParcela parcela : map.parcelas()) {
            Rectangle celda = geometria.limitesCelda(parcela.col(), parcela.fila());
            terrenoDibujar(g, parcela, celda, hierba, estacion);
            if (parcela.cultivo() != null && !parcela.resguardado()) {
                cultivoDibujar(g, parcela, celda, fx);
            }
        }
        for (VistaEdificio edificio : map.edificios()) {
            edificioDibujar(g, edificio, geometria, fx);
        }
        for (VistaCasa casa : casas(map)) {
            vectorCasaDibujar(g, huella(geometria, casa), geometria.tamanioCelda());
        }
        for (VistaParcela parcela : map.parcelas()) {
            if (parcela.cultivo() != null && parcela.resguardado()) {
                cultivoDibujar(g, parcela, geometria.limitesCelda(parcela.col(), parcela.fila()), fx);
            }
        }
        for (VistaNpc npc : npcs(map)) {
            Rectangle celda = geometria.limitesCelda(npc.col(), npc.fila());
            vectorPersonaDibujar(g, celda, camisaNpc(npc.id()), "SOUTH");
            if (!npc.encargoHecho()) {
                globo(g, celda.x + celda.width / 2, celda.y - celda.height + celda.height / 8, Math.max(12, celda.width / 2),
                        iconoEncargo(npc), Paleta.PENDIENTE, 0);
            }
        }
        if (map.avatar() != null) {
            vectorAvatarDibujar(g, celdaAvatar(map.avatar(), geometria, fx), map.avatar().mirada());
        }
    }

        // ------------------------------------------------------------------ personas del valle

    private static List<VistaNpc> npcs(VistaMapa map) {
        return map.npcs() == null ? List.of() : map.npcs();
    }

    private static List<VistaCasa> casas(VistaMapa map) {
        return map.casas() == null ? List.of() : map.casas();
    }

    /** La sombra a los pies de una persona, dentro de su propia parcela. */
    private static Rectangle pies(Rectangle celda) {
        int s = celda.width;
        return new Rectangle(celda.x + s / 5, celda.y + s - s / 5, s * 3 / 5, s / 5);
    }

    /** Dibuja un sprite de persona de una parcela de ancho, parado sobre {@code celda} y tan alto como dice su arte. */
    private void parado(Graphics2D g, BufferedImage sprite, Rectangle celda, int levantar) {
        int s = celda.width;
        int h = sprite.getHeight() * s / sprite.getWidth();
        escalador.draw(g, sprite, celda.x, celda.y + s - h - levantar, s, h);
    }

    private static String iconoEncargo(VistaNpc npc) {
        return "items/" + npc.articuloEncargo() + ".png";
    }

    private static Color camisaNpc(String id) {
        return switch (id) {
            case "MARTA" -> NPC_MARTA;
            case "TOMAS" -> NPC_TOMAS;
            case "LUCIA" -> NPC_LUCIA;
            default -> NPC_INES;
        };
    }

    private void npc(Graphics2D g, VistaNpc npc, GeometriaMapa geo, boolean ambiente, long tiempo, List<Rectangle> sombras,
                     List<Articulo> articulos, List<Runnable> encima) {
        Rectangle celda = geo.limitesCelda(npc.col(), npc.fila());
        int s = celda.width;
        sombras.add(pies(celda));
        Optional<BufferedImage> sprite = recursos.sprite("characters/" + npc.id() + ".png");
        int respirar = ambiente && (tiempo / 600 + npc.col()) % 2 == 0 ? Math.max(1, s / 32) : 0;
        articulos.add(new Articulo(celda.y + celda.height - 0.1, articulos.size(), () -> {
            if (sprite.isPresent()) {
                parado(g, sprite.get(), celda, respirar);
            } else {
                vectorPersonaDibujar(g, celda, camisaNpc(npc.id()), "SOUTH");
            }
        }));
        if (!npc.encargoHecho()) {
            int vaiven = ambiente ? (int) Math.round(Math.sin(tiempo / 350.0 + npc.col()) * s / 12.0) : 0;
            encima.add(() -> globo(g, celda.x + s / 2, celda.y - s + s / 8, Math.max(14, s / 2), iconoEncargo(npc),
                    Paleta.PENDIENTE, vaiven));
        }
    }

        // ------------------------------------------------------------------ avatar

    /** El sprite del avatar mirando hacia una dirección, en un cuadro de su caminata. */
    static String avatarSprite(String mirada, int marco) {
        return "characters/AVATAR_" + mirada.charAt(0) + "_" + marco + ".png";
    }

    /** Dónde se dibuja el avatar: su parcela, o en camino hacia ella mientras camina. */
    private static Rectangle celdaAvatar(VistaAvatar avatar, GeometriaMapa geo, EfectosVisuales fx) {
        Rectangle to = geo.limitesCelda(avatar.col(), avatar.fila());
        int[] from = fx == null ? null : fx.avatarDesde();
        if (from == null) {
            return to;
        }
        Rectangle start = geo.limitesCelda(from[0], from[1]);
        double t = fx.progresoAvatar();
        return new Rectangle((int) Math.round(start.x + (to.x - start.x) * t),
                (int) Math.round(start.y + (to.y - start.y) * t), to.width, to.height);
    }

    private void avatar(Graphics2D g, VistaAvatar avatar, GeometriaMapa geo, EfectosVisuales fx, List<Rectangle> sombras,
                        List<Articulo> articulos) {
        Rectangle celda = celdaAvatar(avatar, geo, fx);
        sombras.add(pies(celda));
        int marco = fx == null ? 0 : fx.marcoAvatar();
        Optional<BufferedImage> sprite = recursos.sprite(avatarSprite(avatar.mirada(), marco));
        articulos.add(new Articulo(celda.y + celda.height - 0.1, articulos.size(), () -> {
            if (sprite.isPresent()) {
                parado(g, sprite.get(), celda, 0);
            } else {
                vectorAvatarDibujar(g, celda, avatar.mirada());
            }
        }));
    }

    /** El avatar sin sprites: un granjero con sombrero de paja, mirando hacia su dirección. */
    private static void vectorAvatarDibujar(Graphics2D g, Rectangle c, String mirada) {
        vectorPersonaDibujar(g, c, CAMISA_AVATAR, mirada);
    }

    /**
     * Una persona dibujada con formas, de una parcela de ancho y dos de alto con los pies en {@code c}: pantalones, una
     * camisa del color dado, una cara, un sombrero de paja y ojos hacia su dirección.
     */
    private static void vectorPersonaDibujar(Graphics2D g, Rectangle c, Color camisa, String mirada) {
        int s = c.width;
        int cx = c.x + s / 2;
        int arriba = c.y - s;
        g.setColor(PANTALONES);
        g.fillRect(cx - s / 5, arriba + s * 3 / 2, s * 2 / 5, s * 2 / 5);
        g.setColor(camisa);
        g.fillRoundRect(cx - s / 4, arriba + s * 19 / 20, s / 2, s * 13 / 20, s / 6, s / 6);
        g.setColor(PIEL_AVATAR);
        g.fillOval(cx - s / 4, arriba + s * 9 / 20, s / 2, s / 2);
        g.setColor(SOMBRERO_AVATAR);
        g.fillRect(cx - s / 3, arriba + s / 2, s * 2 / 3, Math.max(2, s / 12));
        g.fillOval(cx - s / 5, arriba + s * 3 / 10, s * 2 / 5, s / 4);
        if (!"NORTH".equals(mirada)) {
            int desplazar = "EAST".equals(mirada) ? s / 10 : ("WEST".equals(mirada) ? -s / 10 : 0);
            int ojo = Math.max(2, s / 14);
            int ey = arriba + s * 3 / 4;
            g.setColor(OJO_AVATAR);
            if (desplazar == 0) {
                g.fillRect(cx - s / 10 - ojo / 2, ey, ojo, ojo);
                g.fillRect(cx + s / 10 - ojo / 2, ey, ojo, ojo);
            } else {
                g.fillRect(cx + desplazar - ojo / 2, ey, ojo, ojo);
            }
        }
    }

    private void ambienteDibujar(Graphics2D g, EfectosVisuales fx, GeometriaMapa geometria, int width, int height, boolean ambiente) {
        Rectangle area = geometria.area();
        if (ambiente) {
            PintorAmbiente.paint(g, area, geometria.tamanioCelda(), fx.tiempo(), fx.estacion(), fx.clima());
        }
        Color tinte = PaletaEstacion.tinte(fx.clima());
        if (tinte != null) {
            g.setColor(tinte);
            g.fillRect(ambiente ? 0 : area.x, ambiente ? 0 : area.y, ambiente ? width : area.width, ambiente ? height : area.height);
        }
        for (Particula particula : fx.particulas()) {
            int x = area.x + (int) Math.round(particula.x() * area.width);
            int y = area.y + (int) Math.round(particula.y() * area.height);
            if (particula.categoria() == Particula.Categoria.RAIN) {
                g.setColor(RAIN);
                g.drawLine(x, y, x - 3, y + 10);
            } else {
                g.setColor(COPO);
                g.fillOval(x - 2, y - 2, 5, 5);
            }
        }
        indicadorDibujar(g, fx.clima(), area);
        if (ambiente) {
            PintorAmbiente.vineta(g, width, height);
        }
        float noche = fx.alfaNoche();
        if (noche > 0f) {
            g.setColor(new Color(NOCHE.getRed(), NOCHE.getGreen(), NOCHE.getBlue(), Math.round(noche * ALFA_NOCHE_MAXIMA)));
            g.fillRect(0, 0, width, height);
        }
    }

    private void indicadorDibujar(Graphics2D g, String clima, Rectangle area) {
        int r = Math.max(8, area.height / 36);
        int cx = area.x + area.width - 2 * r;
        int cy = area.y + 2 * r;
        g.setStroke(new BasicStroke(Math.max(2f, r / 4f)));
        if ("DROUGHT".equals(clima)) {
            g.setColor(SOL);
            g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
            for (int i = 0; i < 8; i++) {
                double angulo = Math.PI * i / 4;
                g.drawLine(cx + (int) Math.round(Math.cos(angulo) * r * 1.3), cy + (int) Math.round(Math.sin(angulo) * r * 1.3),
                        cx + (int) Math.round(Math.cos(angulo) * r * 1.9), cy + (int) Math.round(Math.sin(angulo) * r * 1.9));
            }
        } else if ("FROST".equals(clima)) {
            g.setColor(COPO);
            for (int i = 0; i < 3; i++) {
                double angulo = Math.PI * i / 3;
                g.drawLine(cx - (int) Math.round(Math.cos(angulo) * r * 1.6), cy - (int) Math.round(Math.sin(angulo) * r * 1.6),
                        cx + (int) Math.round(Math.cos(angulo) * r * 1.6), cy + (int) Math.round(Math.sin(angulo) * r * 1.6));
            }
        }
        g.setStroke(new BasicStroke(1f));
    }

    private void edificioDibujar(Graphics2D g, VistaEdificio edificio, GeometriaMapa geometria, EfectosVisuales fx) {
        Rectangle first = geometria.limitesCelda(edificio.col(), edificio.fila());
        Rectangle ultimo = geometria.limitesCelda(edificio.col() + edificio.width() - 1, edificio.fila() + edificio.height() - 1);
        Rectangle area = first.union(ultimo);
        int margen = Math.max(2, area.height / 16);
        Rectangle body = new Rectangle(area.x + margen, area.y + margen, area.width - 2 * margen, area.height - 2 * margen);
        Optional<BufferedImage> hoja = recursos.sprite("buildings/" + edificio.tipo() + ".png");
        if (hoja.isPresent()) {
            g.drawImage(hoja.get(), area.x, area.y, area.width, area.height, null);
            spritesAnimalDibujar(g, edificio, area, fx);
            dibujarPendiente(g, edificio, area);
            return;
        }
        if ("GREENHOUSE".equals(edificio.tipo())) {
            g.setColor(Paleta.VIDRIO_INVERNADERO);
            g.fillRect(body.x, body.y, body.width, body.height);
            g.setColor(Paleta.CONTORNO_EDIFICIO);
            for (int i = 1; i < edificio.width(); i++) {
                int x = body.x + body.width * i / edificio.width();
                g.drawLine(x, body.y, x, body.y + body.height);
            }
            for (int i = 1; i < edificio.height(); i++) {
                int y = body.y + body.height * i / edificio.height();
                g.drawLine(body.x, y, body.x + body.width, y);
            }
        } else {
            g.setColor(colorPared(edificio.tipo()));
            g.fillRect(body.x, body.y, body.width, body.height);
            int altoTecho = body.height / 3;
            g.setColor(Paleta.TECHO);
            g.fillPolygon(new int[] {body.x - margen, body.x + body.width / 2, body.x + body.width + margen},
                    new int[] {body.y + altoTecho, body.y - margen, body.y + altoTecho}, 3);
        }
        g.setColor(Paleta.CONTORNO_EDIFICIO);
        g.drawRect(body.x, body.y, body.width, body.height);
        animalesDibujar(g, edificio, body, fx);
        dibujarPendiente(g, edificio, area);
    }

    private void dibujarPendiente(Graphics2D g, VistaEdificio edificio, Rectangle area) {
        if (edificio.productosPendientes() > 0) {
            int r = Math.max(4, area.width / 12);
            g.setColor(Paleta.PENDIENTE);
            g.fillOval(area.x + area.width - 2 * r, area.y, 2 * r, 2 * r);
            g.setColor(Paleta.CONTORNO_EDIFICIO);
            g.drawOval(area.x + area.width - 2 * r, area.y, 2 * r, 2 * r);
        }
    }

    /**
     * Animales frente a un edificio dibujados con un sprite: un sprite del tamaño de una baldosa cada uno, con una
     * insignia si no están bien.
     */
    private void spritesAnimalDibujar(Graphics2D g, VistaEdificio edificio, Rectangle area, EfectosVisuales fx) {
        int ranuras = Math.max(1, edificio.capacidad());
        int d = Math.max(8, Math.min(area.width / ranuras, area.height / 2));
        for (VistaAnimal animal : edificio.animales()) {
            int marco = fx == null ? 0 : fx.marcoAnimal(animal.indice());
            Optional<BufferedImage> sprite = recursos.sprite("animals/" + animal.tipo() + "_" + marco + ".png");
            int x = area.x + animal.indice() * area.width / ranuras + (area.width / ranuras - d) / 2;
            int y = area.y + area.height - d - Math.max(1, area.height / 32);
            if (sprite.isPresent()) {
                g.drawImage(sprite.get(), x, y, d, d, null);
            } else {
                vectorAnimalDibujar(g, animal, x, y, d, marco);
            }
            insigniaDibujar(g, animal, x + d, y, d);
        }
    }

    private static void insigniaDibujar(Graphics2D g, VistaAnimal animal, int derecha, int arriba, int d) {
        boolean enfermo = "SICK".equals(animal.condicion());
        if (!enfermo && !"HUNGRY".equals(animal.condicion())) {
            return;
        }
        int r = Math.max(3, d / 5);
        int x = derecha - 2 * r;
        g.setColor(enfermo ? Paleta.ANIMAL_ENFERMO : Paleta.ANIMAL_HAMBRIENTO);
        g.fillOval(x, arriba, 2 * r, 2 * r);
        g.setColor(Paleta.CONTORNO_EDIFICIO);
        g.drawOval(x, arriba, 2 * r, 2 * r);
        if (enfermo) {
            g.drawLine(x + r, arriba + r / 2, x + r, arriba + 2 * r - r / 2);
            g.drawLine(x + r / 2, arriba + r, x + 2 * r - r / 2, arriba + r);
        }
    }

    private static Color colorPared(String tipo) {
        return switch (tipo) {
            case "BARN" -> Paleta.PARED_ESTABLO;
            case "WAREHOUSE" -> Paleta.PARED_ALMACEN;
            default -> Paleta.PARED_GALLINERO;
        };
    }

    private void animalesDibujar(Graphics2D g, VistaEdificio edificio, Rectangle body, EfectosVisuales fx) {
        int ranuras = Math.max(1, edificio.capacidad());
        int d = Math.max(4, body.width / (ranuras + 1));
        for (VistaAnimal animal : edificio.animales()) {
            int marco = fx == null ? 0 : fx.marcoAnimal(animal.indice());
            int x = body.x + body.width * (2 * animal.indice() + 1) / (2 * ranuras) - d / 2;
            int y = body.y + body.height * 2 / 3 - d / 2 - vaiven(marco, d);
            Optional<BufferedImage> sprite = recursos.sprite("animals/" + animal.tipo() + "_" + marco + ".png");
            if (sprite.isPresent()) {
                g.drawImage(sprite.get(), x, y, d, d, null);
                continue;
            }
            g.setColor(switch (animal.condicion()) {
                case "SICK" -> Paleta.ANIMAL_ENFERMO;
                case "HUNGRY" -> Paleta.ANIMAL_HAMBRIENTO;
                default -> Paleta.ANIMAL_SANO;
            });
            animalVectorPintar(g, animal, x, y, d);
        }
    }

    private static void vectorAnimalDibujar(Graphics2D g, VistaAnimal animal, int x, int y, int d, int marco) {
        g.setColor(switch (animal.condicion()) {
            case "SICK" -> Paleta.ANIMAL_ENFERMO;
            case "HUNGRY" -> Paleta.ANIMAL_HAMBRIENTO;
            default -> Paleta.ANIMAL_SANO;
        });
        animalVectorPintar(g, animal, x, y - vaiven(marco, d), d);
    }

    private static void animalVectorPintar(Graphics2D g, VistaAnimal animal, int x, int y, int d) {
        if ("COW".equals(animal.tipo())) {
            g.fillOval(x - d / 4, y, d + d / 2, d);
            g.setColor(Paleta.CONTORNO_EDIFICIO);
            g.fillOval(x + d / 4, y + d / 4, d / 3, d / 3);
        } else {
            g.fillOval(x, y, d, d);
            g.setColor(Paleta.PARED_ESTABLO);
            g.fillOval(x + d / 3, y - d / 5, d / 3, d / 3);
        }
    }

    /** Cuántos píxeles sube el animal en cada cuadro de su saltito: abajo, arriba, a medio camino. */
    private static int vaiven(int marco, int d) {
        return switch (marco) {
            case 1 -> Math.max(1, d / 6);
            case 2 -> Math.max(1, d / 12);
            default -> 0;
        };
    }

    private void terrenoDibujar(Graphics2D g, VistaParcela parcela, Rectangle c, Color hierba, String estacion) {
        String name = switch (parcela.terreno()) {
            case "SOIL" -> parcela.regado() ? "terrain/SOIL_WATERED.png" : "terrain/SOIL.png";
            case "WATER" -> "terrain/WATER.png";
            case "OBSTACLE" -> "terrain/OBSTACLE.png";
            case "PATH" -> "terrain/PATH.png";
            default -> "terrain/GRASS_" + estacion + ".png";
        };
        Optional<BufferedImage> sprite = recursos.sprite(name);
        if (sprite.isPresent()) {
            g.drawImage(sprite.get(), c.x, c.y, c.width, c.height, null);
            return;
        }
        switch (parcela.terreno()) {
            case "SOIL" -> {
                g.setColor(parcela.regado() ? Paleta.TIERRA_REGADA : Paleta.SOIL);
                g.fillRect(c.x, c.y, c.width, c.height);
                g.setColor(Paleta.SURCO);
                for (int i = 1; i <= 2; i++) {
                    int y = c.y + c.height * i / 3;
                    g.drawLine(c.x + c.width / 8, y, c.x + c.width * 7 / 8, y);
                }
            }
            case "WATER" -> {
                g.setColor(Paleta.WATER);
                g.fillRect(c.x, c.y, c.width, c.height);
                g.setColor(Paleta.ONDA);
                int y = c.y + c.height / 2;
                g.drawArc(c.x + c.width / 6, y - c.height / 10, c.width / 3, c.height / 5, 0, 180);
                g.drawArc(c.x + c.width / 2, y - c.height / 10, c.width / 3, c.height / 5, 0, 180);
            }
            case "OBSTACLE" -> vectorObstaculoDibujar(g, c);
            case "PATH" -> {
                g.setColor(PATH);
                g.fillRect(c.x, c.y, c.width, c.height);
            }
            default -> {
                g.setColor(hierba);
                g.fillRect(c.x, c.y, c.width, c.height);
            }
        }
    }

    private static void vectorObstaculoDibujar(Graphics2D g, Rectangle c) {
        g.setColor(Paleta.OBSTACLE);
        g.fillRect(c.x, c.y, c.width, c.height);
        g.setColor(Paleta.TRONCO);
        g.fillRect(c.x + c.width * 9 / 20, c.y + c.height / 2, c.width / 10, c.height * 2 / 5);
        g.setColor(Paleta.HOJA);
        g.fillOval(c.x + c.width / 5, c.y + c.height / 8, c.width * 3 / 5, c.height * 3 / 5);
    }

    private void cultivoDibujar(Graphics2D g, VistaParcela parcela, Rectangle c, EfectosVisuales fx) {
        float scale = fx == null ? 1f : fx.escalaCultivo(parcela.col(), parcela.fila());
        AffineTransform archivado = g.getTransform();
        if (scale < 1f) {
            double cx = c.x + c.width / 2.0;
            double cy = c.y + c.height / 2.0;
            g.translate(cx, cy);
            g.scale(scale, scale);
            g.translate(-cx, -cy);
        }
        Optional<BufferedImage> sprite = recursos.sprite("crops/" + parcela.cultivo() + "_" + parcela.etapa() + ".png");
        if (sprite.isPresent()) {
            g.drawImage(sprite.get(), c.x, c.y, c.width, c.height, null);
        } else {
            vectorCultivoDibujar(g, parcela, c);
        }
        g.setTransform(archivado);
    }

    private void vectorCultivoDibujar(Graphics2D g, VistaParcela parcela, Rectangle c) {
        int s = c.width;
        int cx = c.x + s / 2;
        int cy = c.y + s / 2;
        g.setStroke(new BasicStroke(Math.max(1f, s / 24f)));
        switch (parcela.etapa()) {
            case "SEED" -> {
                g.setColor(Paleta.SEED);
                int r = Math.max(2, Math.round(s * 0.08f));
                g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
            }
            case "SPROUT" -> {
                g.setColor(Paleta.HOJA);
                g.drawLine(cx, cy + s / 6, cx - Math.round(s * 0.12f), cy - s / 20);
                g.drawLine(cx, cy + s / 6, cx + Math.round(s * 0.12f), cy - s / 20);
            }
            case "GROWING" -> plantaDibujar(g, cx, cy, s, false, parcela);
            case "MATURE" -> plantaDibujar(g, cx, cy, s, true, parcela);
            default -> {
                g.setColor(Paleta.WITHERED);
                int d = Math.round(s * 0.18f);
                g.drawLine(cx - d, cy - d, cx + d, cy + d);
                g.drawLine(cx - d, cy + d, cx + d, cy - d);
            }
        }
        g.setStroke(new BasicStroke(1f));
    }

    private void plantaDibujar(Graphics2D g, int cx, int cy, int s, boolean conFruta, VistaParcela parcela) {
        int arriba = cy - Math.round(s * TALLO);
        g.setColor(Paleta.HOJA);
        g.drawLine(cx, cy + Math.round(s * TALLO), cx, arriba);
        int lw = Math.round(s * LEAF_W);
        int lh = Math.round(s * LEAF_H);
        g.fillOval(cx - lw, cy - lh, lw, lh);
        g.fillOval(cx, cy, lw, lh);
        g.fillOval(cx - lw, cy + lh, lw, lh);
        if (conFruta) {
            int r = Math.round(s * FRUTA);
            g.setColor(Paleta.cultivo(parcela.cultivo()));
            g.fillOval(cx - r, arriba - r, 2 * r, 2 * r);
        }
    }
}
