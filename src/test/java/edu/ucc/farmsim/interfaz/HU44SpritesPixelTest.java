package edu.ucc.farmsim.interfaz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaMapa;
import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.interfaz.dibujo.DibujanteMapa;
import edu.ucc.farmsim.interfaz.dibujo.GeometriaMapa;
import edu.ucc.farmsim.interfaz.recursos.GestorRecurso;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** HU-44: nuestros propios sprites de pixel art de 32 x 32, dibujados nítidos a escalas enteras. */
class HU44SpritesPixelTest {

    private static final Path RECURSOS = Path.of("src/main/resources/assets");
    /**
     * Sprites altos: los edificios tienen una baldosa de techo sobre el suelo que usan, los árboles miden dos baldosas
     * de alto.
     */
    private static final Map<String, int[]> LAMINAS = Map.of(
            "buildings/COOP.png", new int[] {64, 96}, "buildings/BARN.png", new int[] {96, 96},
            "buildings/WAREHOUSE.png", new int[] {64, 96}, "buildings/GREENHOUSE.png", new int[] {96, 128});

    private static int[] hoja(String name) {
        if (name.startsWith("terrain/TREE_") || name.startsWith("terrain/PINE_")) {
            return new int[] {64, 64};
        }
        if (name.startsWith("characters/")) {
            return new int[] {32, 64};
        }
        if (name.startsWith("village/")) {
            return name.equals("village/SHOP.png") || name.equals("village/CLINIC.png") ? new int[] {160, 128}
                    : new int[] {128, 128};
        }
        return LAMINAS.get(name);
    }

    private static List<Path> imagenes() throws IOException {
        try (Stream<Path> archivos = Files.walk(RECURSOS)) {
            return archivos.filter(p -> p.toString().endsWith(".png")).toList();
        }
    }

    private static String rel(Path p) {
        return RECURSOS.relativize(p).toString().replace('\\', '/');
    }

    @Test
    void HU44_ca1_everyImageIs32x32AndTheBuildingSheetsAreWholeTiles() throws IOException {
        for (Path path : imagenes()) {
            BufferedImage imagen = ImageIO.read(path.toFile());
            int[] hoja = hoja(rel(path));
            if (hoja == null) {
                assertEquals(32, imagen.getWidth(), rel(path));
                assertEquals(32, imagen.getHeight(), rel(path));
            } else {
                assertEquals(hoja[0], imagen.getWidth(), rel(path));
                assertEquals(hoja[1], imagen.getHeight(), rel(path));
            }
        }
    }

    @Test
    void HU44_ca2_el_paquete_cubre_terrenos_cultivos_animales_edificios_articulos_personajes_y_iconos() throws IOException {
        Set<String> tener = new HashSet<>();
        for (Path path : imagenes()) {
            tener.add(rel(path));
        }
        List<String> necesitar = new ArrayList<>(List.of("terrain/SOIL.png", "terrain/SOIL_WATERED.png", "terrain/WATER.png",
                "terrain/OBSTACLE.png", "buildings/COOP.png", "buildings/BARN.png", "buildings/WAREHOUSE.png",
                "buildings/GREENHOUSE.png", "icons/COIN.png", "icons/ENERGY.png", "terrain/PATH.png",
                "village/HOME.png", "village/SHOP.png", "village/FARMHOUSE.png", "village/CLINIC.png",
                "village/COTTAGE.png"));
        for (String estacion : List.of("SPRING", "SUMMER", "AUTUMN", "WINTER")) {
            necesitar.add("terrain/GRASS_" + estacion + ".png");
            necesitar.add("icons/" + estacion + ".png");
        }
        for (String cultivo : List.of("CARROT", "POTATO", "TOMATO", "CORN", "PUMPKIN")) {
            for (String etapa : List.of("SEED", "SPROUT", "GROWING", "MATURE", "WITHERED")) {
                necesitar.add("crops/" + cultivo + "_" + etapa + ".png");
            }
        }
        for (String animal : List.of("HEN", "COW")) {
            for (int marco = 0; marco < 3; marco++) {
                necesitar.add("animals/" + animal + "_" + marco + ".png");
            }
        }
        for (TipoArticulo articulo : TipoArticulo.values()) {
            necesitar.add("items/" + articulo.name() + ".png");
        }
        for (String direccion : List.of("S", "N", "E", "W")) {
            for (int marco = 0; marco < 3; marco++) {
                necesitar.add("characters/AVATAR_" + direccion + "_" + marco + ".png");
            }
        }
        for (String npc : List.of("MARTA", "TOMAS", "LUCIA", "INES")) {
            necesitar.add("characters/" + npc + ".png");
        }
        for (String name : List.of("SOW", "WATER", "HARVEST", "BUILD", "FEED", "SUNNY", "CLOUDY", "RAINY", "DROUGHT",
                "FROST", "SNOWY")) {
            necesitar.add("icons/" + name + ".png");
        }
        for (String archivo : necesitar) {
            assertTrue(tener.contains(archivo), "missing sprite " + archivo);
        }
    }

    @Test
    void HU44_ca2_el_mapa_dibuja_el_real_paquete_en_lugar_de_los_vectores() throws IOException {
        BufferedImage imagen = paint(new DibujanteMapa(), mapaTierra());
        BufferedImage sprite = ImageIO.read(RECURSOS.resolve("terrain/SOIL.png").toFile());
        var celda = GeometriaMapa.ajustar(16, 10, 1024, 640).limitesCelda(2, 2);

        assertEquals(64, celda.width);
        assertEquals(sprite.getRGB(5, 5), imagen.getRGB(celda.x + 10, celda.y + 10));
        assertEquals(sprite.getRGB(20, 9), imagen.getRGB(celda.x + 40, celda.y + 18));
    }

    @Test
    void HU44_ca3_theCellSnapsToAWholeScaleWhenItWastesLittleAndAScaleOf2FitsExactly() {
        assertEquals(64, GeometriaMapa.ajustar(16, 10, 1024, 640).tamanioCelda());
        assertEquals(64, GeometriaMapa.ajustar(16, 10, 1100, 700).tamanioCelda());
        assertEquals(96, GeometriaMapa.ajustar(16, 10, 1536, 960).tamanioCelda());
        assertEquals(61, GeometriaMapa.ajustar(16, 10, 1024, 618).tamanioCelda(), "a whole scale would waste too much");
        assertEquals(32, GeometriaMapa.ajustar(16, 10, 512, 320).tamanioCelda());
        assertEquals(20, GeometriaMapa.ajustar(16, 10, 320, 200).tamanioCelda(), "smaller than one tile: any scale");
    }

    @Test
    void HU44_ca3_aSpriteScaledByAWholeFactorKeepsOnlyItsOwnColours() throws IOException {
        BufferedImage mitad = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                mitad.setRGB(x, y, x < 16 ? 0xFFFF0000 : 0xFF0000FF);
            }
        }
        ByteArrayOutputStream fuera = new ByteArrayOutputStream();
        ImageIO.write(mitad, "png", fuera);
        GestorRecurso recursos = new GestorRecurso(name -> name.equals("terrain/SOIL.png")
                ? new ByteArrayInputStream(fuera.toByteArray()) : fallando(name));

        BufferedImage imagen = paint(new DibujanteMapa(recursos), mapaTierra());

        var celda = GeometriaMapa.ajustar(16, 10, 1024, 640).limitesCelda(3, 3);
        Set<Integer> tonos = new HashSet<>();
        for (int y = 1; y < celda.height - 1; y++) {
            for (int x = 1; x < celda.width - 1; x++) {
                tonos.add(imagen.getRGB(celda.x + x, celda.y + y));
            }
        }
        assertEquals(Set.of(0xFFFF0000, 0xFF0000FF), tonos);
    }

    @Test
    void HU44_ca4_aMissingSpriteStillDrawsTheVectorGraphic() {
        BufferedImage imagen = paint(new DibujanteMapa(GestorRecurso.soloVector()), mapaTierra());
        var celda = GeometriaMapa.ajustar(16, 10, 1024, 640).limitesCelda(2, 2);

        assertEquals(0xFF8B5A2B, imagen.getRGB(celda.x + 2, celda.y + 2));
    }

    @Test
    void HU44_ca5_la_guion_y_la_licencia_archivo_existir_y_cada_carpeta_es_listado() throws IOException {
        assertTrue(Files.exists(Path.of("tools/gen_sprites.py")));
        String listado = Files.readString(Path.of("docs/ASSETS.md"));
        for (String carpeta : List.of("terrain/", "crops/", "animals/", "buildings/", "items/", "characters/", "icons/")) {
            assertTrue(listado.contains(carpeta), "ASSETS.md must list " + carpeta);
        }
        assertTrue(listado.contains("trabajo propio"));
        assertTrue(listado.contains("gen_sprites.py"));
    }

    private static java.io.InputStream fallando(String name) throws FileNotFoundException {
        throw new FileNotFoundException(name);
    }

    private static VistaMapa mapaTierra() {
        List<VistaParcela> parcelas = new ArrayList<>();
        for (int fila = 0; fila < 10; fila++) {
            for (int col = 0; col < 16; col++) {
                parcelas.add(new VistaParcela(col, fila, "SOIL", null, null, false, 0));
            }
        }
        return new VistaMapa(16, 10, parcelas, List.of());
    }

    private static BufferedImage paint(DibujanteMapa dibujante, VistaMapa map) {
        BufferedImage imagen = new BufferedImage(1024, 640, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagen.createGraphics();
        dibujante.renderizar(g, map, 1024, 640, null);
        g.dispose();
        return imagen;
    }
}
