package edu.ucc.farmsim.interfaz.dibujo;

import edu.ucc.farmsim.interfaz.recursos.GestorRecurso;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Optional;

/**
 * La imagen viva detrás de la pantalla de inicio: una mañana de primavera sobre el valle, con un cielo que se desvanece
 * hacia el horizonte, nubes y dos filas de colinas que se desplazan a distintas velocidades, pájaros, y al frente una
 * franja de granja con su cerca, árboles, establo, gallinero y cultivos, donde las gallinas picotean y el granjero pasa
 * caminando. Toda posición es una función del tiempo, así que no necesita estado y se dibuja igual en cualquier {@link
 * Graphics2D}.
 *
 * <p>Stories: HU-33, HU-48.
 */
public final class EscenaTitulo {

    private static final Color ARRIBA_CIELO = new Color(0x4FA8E8);
    private static final Color CIELO_BAJO = new Color(0xD4F0FF);
    private static final Color SOL = new Color(0xFFF2B0);
    private static final Color NUBE = new Color(0xFFFFFF);
    private static final Color SOMBREADO_NUBE = new Color(0xD6E8F5);
    private static final Color LEJOS_COLINA = new Color(0x9CCB7A);
    private static final Color CERCA_COLINA = new Color(0x6DAE4E);
    private static final Color AVE = new Color(0x2E3A4A);
    private static final Color DESVANECER = new Color(0x1A1008);
    private static final int MS_DESVANECER = 900;
    private static final int NUBES = 6;
    private static final int AVES = 4;
    private static final int GALLINAS = 3;

    private final GestorRecurso recursos;
    private final EscaladorPixel escalador = new EscaladorPixel();

    /**
     * Crea la escena.
     *
     * @param recursos de donde vienen los sprites; sin ellos la tierra se dibuja con formas simples
     */
    public EscenaTitulo(GestorRecurso recursos) {
        this.recursos = recursos;
    }

    /**
     * Pinta la escena.
     *
     * @param g      dónde pintar
     * @param width  ancho del área
     * @param height alto del área
     * @param tiempo   milisegundos desde que apareció la escena
     */
    public void paint(Graphics2D g, int width, int height, long tiempo) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        int horizonte = height * 53 / 100;
        g.setPaint(new GradientPaint(0, 0, ARRIBA_CIELO, 0, horizonte, CIELO_BAJO));
        g.fillRect(0, 0, width, height);
        sol(g, width, height);
        nubes(g, width, height, tiempo);
        aves(g, width, height, tiempo);
        colina(g, width, height, horizonte - height / 9, LEJOS_COLINA, 210.0, height / 26.0, tiempo / 90.0);
        colina(g, width, height, horizonte, CERCA_COLINA, 140.0, height / 30.0, tiempo / 45.0);
        tierras(g, width, height, horizonte, tiempo);
        if (tiempo < MS_DESVANECER) {
            int alfa = (int) Math.round(255 * (1 - tiempo / (double) MS_DESVANECER));
            g.setColor(new Color(DESVANECER.getRed(), DESVANECER.getGreen(), DESVANECER.getBlue(), Math.max(0, Math.min(255, alfa))));
            g.fillRect(0, 0, width, height);
        }
    }

    private static void sol(Graphics2D g, int width, int height) {
        int r = Math.max(20, height / 14);
        int cx = width * 80 / 100;
        int cy = height * 16 / 100;
        for (int anillo = 3; anillo >= 1; anillo--) {
            g.setColor(new Color(255, 245, 190, 40));
            int rr = r + anillo * r / 2;
            g.fillOval(cx - rr, cy - rr, 2 * rr, 2 * rr);
        }
        g.setColor(SOL);
        g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
    }

    private static void nubes(Graphics2D g, int width, int height, long tiempo) {
        int unidad = Math.max(6, height / 90);
        int tramo = width + 40 * unidad;
        for (int i = 0; i < NUBES; i++) {
            double velocidad = unidad * (0.6 + (i % 3) * 0.35);
            int x = (int) Math.floorMod((long) (tiempo / 1000.0 * velocidad + i * tramo / (double) NUBES), (long) tramo) - 20 * unidad;
            int y = height * (6 + (i * 37) % 30) / 100;
            int size = 3 + i % 3;
            g.setColor(SOMBREADO_NUBE);
            g.fillRoundRect(x, y + unidad * 2, unidad * size * 5, unidad * 3, unidad * 2, unidad * 2);
            g.setColor(NUBE);
            g.fillRoundRect(x + unidad, y + unidad, unidad * size * 5 - 2 * unidad, unidad * 3, unidad * 2, unidad * 2);
            g.fillRoundRect(x + unidad * size, y - unidad, unidad * size * 2, unidad * 3, unidad * 2, unidad * 2);
            g.fillRoundRect(x + unidad * size * 2, y - unidad * 2, unidad * size * 2, unidad * 3, unidad * 2, unidad * 2);
        }
    }

    private static void aves(Graphics2D g, int width, int height, long tiempo) {
        g.setColor(AVE);
        int ala = Math.max(4, height / 120);
        for (int i = 0; i < AVES; i++) {
            int tramo = width + 200;
            int x = (int) Math.floorMod(tiempo / 18 + i * 140L, (long) tramo) - 100;
            int y = height * 22 / 100 + i * ala * 3 + (int) (Math.sin(tiempo / 400.0 + i) * ala);
            boolean up = ((tiempo / 180) + i) % 2 == 0;
            int consejo = up ? -ala : ala / 2;
            g.drawLine(x - ala * 2, y + consejo, x, y);
            g.drawLine(x, y, x + ala * 2, y + consejo);
        }
    }

    private static void colina(Graphics2D g, int width, int height, int base, Color color, double onda, double subir,
                             double desplazar) {
        Polygon colina = new Polygon();
        colina.addPoint(0, height);
        for (int x = 0; x <= width + 8; x += 8) {
            double v = Math.sin((x + desplazar) / onda) * subir + Math.sin((x + desplazar) / (onda / 2.3)) * subir / 3;
            colina.addPoint(x, base - (int) Math.round(v));
        }
        colina.addPoint(width, height);
        g.setColor(color);
        g.fillPolygon(colina);
    }

    private void tierras(Graphics2D g, int width, int height, int horizonte, long tiempo) {
        int s = Math.max(32, Math.round(height / 15f / 8f) * 8);
        int arriba = horizonte + s / 4;
        Optional<BufferedImage> hierba = recursos.sprite("terrain/GRASS_SPRING.png");
        if (hierba.isEmpty()) {
            g.setColor(CERCA_COLINA.darker());
            g.fillRect(0, arriba, width, height - arriba);
            return;
        }
        for (int fila = 0; arriba + fila * s < height; fila++) {
            for (int col = 0; col * s < width; col++) {
                int variante = CapaSuelo.variante(col, fila + 50);
                BufferedImage baldosa = recursos.sprite(CapaSuelo.nombreHierba("SPRING", variante)).orElse(hierba.get());
                escalador.draw(g, baldosa, col * s, arriba + fila * s, s, s);
            }
        }
        int arribaTierra = arriba + s * 2;
        int izquierdaTierra = width * 30 / 100;
        int colsTierra = Math.max(3, width * 32 / 100 / s);
        recursos.sprite("terrain/SOIL.png").ifPresent(tierra -> {
            for (int fila = 0; fila < 2; fila++) {
                for (int col = 0; col < colsTierra; col++) {
                    escalador.draw(g, tierra, izquierdaTierra + col * s, arribaTierra + fila * s, s, s);
                }
            }
        });
        String[] cultivos = {"CARROT_MATURE", "CORN_MATURE", "PUMPKIN_MATURE", "TOMATO_MATURE", "POTATO_MATURE"};
        recursos.sprite("terrain/TREE_SPRING.png").ifPresent(arbol -> {
            for (int[] at : new int[][] {{2, -1}, {7, -2}, {85, -1}, {93, -2}}) {
                sombra(g, width * at[0] / 100 + s / 2, arriba + s + at[1] * s / 3, s);
                escalador.draw(g, arbol, width * at[0] / 100 - s / 2, arriba - s + at[1] * s / 3, 2 * s, 2 * s);
            }
        });
        recursos.sprite("terrain/FENCE_H.png").ifPresent(cerca -> {
            for (int x = 0; x < width; x += s) {
                escalador.draw(g, cerca, x, arriba + s / 4, s, s);
            }
        });
        edificio(g, "buildings/BARN.png", width * 12 / 100, arriba + s * 3, s);
        edificio(g, "buildings/COOP.png", width * 70 / 100, arriba + s * 3, s);
        for (int fila = 0; fila < 2; fila++) {
            for (int col = 0; col < colsTierra; col++) {
                String name = cultivos[(col + fila * 2) % cultivos.length];
                int x = izquierdaTierra + col * s;
                int y = arribaTierra + fila * s;
                recursos.sprite("crops/" + name + ".png").ifPresent(cultivo -> escalador.draw(g, cultivo, x, y, s, s));
            }
        }
        for (int i = 0; i < GALLINAS; i++) {
            double fase = tiempo / 2600.0 + i * 2.1;
            int x = width * (64 + i * 9) / 100 + (int) Math.round(Math.sin(fase) * s * 1.2);
            int y = arriba + s * 3 + s / 4 + i * s / 3;
            int marco = (int) ((tiempo / 250 + i) % 3);
            boolean izquierda = Math.cos(fase) < 0;
            sombra(g, x + s / 2, y + s - s / 8, s * 2 / 3);
            recursos.sprite("animals/HEN_" + marco + ".png").ifPresent(gallina -> escalador.draw(g, gallina, x, y, izquierda ? -s : s, s));
        }
        int walk = (int) Math.floorMod(tiempo / 14, (long) (width + 4 * s)) - 2 * s;
        int paso = (int) ((tiempo / 160) % 3);
        int farmerY = arriba + s * 4 + s / 3;
        sombra(g, walk + s / 2, farmerY + s - s / 8, s * 2 / 3);
        recursos.sprite("characters/AVATAR_E_" + paso + ".png").ifPresent(avatar -> escalador.draw(g, avatar, walk, farmerY, s, s));
    }

    private void edificio(Graphics2D g, String name, int x, int inferior, int s) {
        recursos.sprite(name).ifPresent(img -> {
            int w = img.getWidth() * s / GeometriaMapa.BALDOSA;
            int h = img.getHeight() * s / GeometriaMapa.BALDOSA;
            sombra(g, x + w / 2, inferior - s / 6, w + s / 2);
            escalador.draw(g, img, x, inferior - h, w, h);
        });
    }

    private static void sombra(Graphics2D g, int cx, int cy, int w) {
        g.setColor(new Color(0, 0, 0, 50));
        g.fillOval(cx - w / 2, cy - w / 8, w, w / 4);
    }
}
