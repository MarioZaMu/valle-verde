package edu.ucc.farmsim.interfaz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaEdificio;
import edu.ucc.farmsim.aplicacion.dto.VistaMapa;
import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
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
import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** HU-42: los sprites se cargan una sola vez mediante el {@link GestorRecurso}, y uno que falte recurre a vectores. */
class HU42GestorRecursoTest {

    private static final int ROJO = 0xFFFF0000;

    private static byte[] png(int color) throws IOException {
        BufferedImage imagen = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                imagen.setRGB(x, y, color);
            }
        }
        ByteArrayOutputStream fuera = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", fuera);
        return fuera.toByteArray();
    }

    private static VistaMapa granja() {
        List<VistaParcela> parcelas = new ArrayList<>();
        for (int fila = 0; fila < 10; fila++) {
            for (int col = 0; col < 16; col++) {
                parcelas.add(col == 5 && fila == 3
                        ? new VistaParcela(col, fila, "SOIL", "CARROT", "MATURE", false, 0)
                        : new VistaParcela(col, fila, "SOIL", null, null, false, 0));
            }
        }
        return new VistaMapa(16, 10, parcelas, List.<VistaEdificio>of());
    }

    private static BufferedImage paint(DibujanteMapa dibujante) {
        BufferedImage imagen = new BufferedImage(1024, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagen.createGraphics();
        dibujante.renderizar(g, granja(), 1024, 600, null);
        g.dispose();
        return imagen;
    }

    @Test
    void HU42_ca1_el_mismo_sprite_pedido_dos_veces_es_leer_una_vez_y_es_el_mismo_objeto() throws IOException {
        int[] lee = {0};
        byte[] datos = png(ROJO);
        GestorRecurso recursos = new GestorRecurso(path -> {
            lee[0]++;
            return new ByteArrayInputStream(datos);
        });

        Optional<BufferedImage> first = recursos.sprite("crops/CARROT_MATURE.png");
        Optional<BufferedImage> segundo = recursos.sprite("crops/CARROT_MATURE.png");

        assertTrue(first.isPresent());
        assertSame(first.get(), segundo.get());
        assertEquals(1, lee[0]);
        assertEquals(1, recursos.lee());
    }

    @Test
    void HU42_ca2_aMissingFileGivesNoSpriteNoExceptionAndOneWarning() {
        List<LogRecord> registrado = new ArrayList<>();
        Logger registrador = Logger.getLogger(GestorRecurso.class.getName());
        Handler manejador = new Handler() {
            @Override
            public void publish(LogRecord record) {
                registrado.add(record);
            }

            @Override
            public void flush() { }

            @Override
            public void close() { }
        };
        registrador.addHandler(manejador);
        try {
            GestorRecurso recursos = new GestorRecurso(path -> {
                throw new FileNotFoundException(path);
            });

            assertTrue(recursos.sprite("animals/HEN_0.png").isEmpty());
            assertTrue(recursos.sprite("animals/HEN_0.png").isEmpty());

            assertEquals(1, registrado.stream().filter(r -> r.getLevel() == Level.WARNING).count());
            assertEquals(1, recursos.lee());
        } finally {
            registrador.removeHandler(manejador);
        }
    }

    @Test
    void HU42_ca2_un_ilegible_imagen_tambien_cae_atras() {
        GestorRecurso recursos = new GestorRecurso(path -> new ByteArrayInputStream(new byte[] {1, 2, 3}));

        assertTrue(recursos.sprite("crops/broken.png").isEmpty());
    }

    @Test
    void HU42_ca2_aMissingSpriteDrawsTheVectorGraphicInstead() {
        GestorRecurso nada = new GestorRecurso(path -> {
            throw new FileNotFoundException(path);
        });

        BufferedImage conRespaldo = paint(new DibujanteMapa(nada));
        BufferedImage vectores = paint(new DibujanteMapa(GestorRecurso.soloVector()));

        assertEquals(pixelesDe(vectores), pixelesDe(conRespaldo));
    }

    @Test
    void HU42_ca3_sin_cualquier_imagen_el_juego_es_dibujado_completamente_con_vectores() {
        BufferedImage imagen = paint(new DibujanteMapa(GestorRecurso.soloVector()));

        var geometria = GeometriaMapa.ajustar(16, 10, 1024, 600);
        var celda = geometria.limitesCelda(5, 3);
        int[] tierra = new int[celda.width * celda.height];
        imagen.getRGB(celda.x, celda.y, celda.width, celda.height, tierra, 0, celda.width);
        boolean frutaTiene = false;
        for (int pixel : tierra) {
            frutaTiene |= pixel == (0xFF000000 | 0xF28C28);
        }
        assertTrue(frutaTiene, "the carrot is drawn with vectors");
    }

    @Test
    void HU42_aSpriteThatExistsReplacesTheVectorGraphic() throws IOException {
        byte[] datos = png(ROJO);
        GestorRecurso recursos = new GestorRecurso(path -> {
            if (!path.equals("crops/CARROT_MATURE.png")) {
                throw new FileNotFoundException(path);
            }
            return new ByteArrayInputStream(datos);
        });

        BufferedImage imagen = paint(new DibujanteMapa(recursos));

        var celda = GeometriaMapa.ajustar(16, 10, 1024, 600).limitesCelda(5, 3);
        assertEquals(ROJO, imagen.getRGB(celda.x + celda.width / 2, celda.y + celda.height / 2));
    }

    @Test
    void HU42_el_classpath_cargador_lee_los_recursos_carpeta_y_sobrevive_cuando_eso_es_vacio() {
        GestorRecurso recursos = GestorRecurso.desdeClasspath();

        assertTrue(recursos.sprite("does/not/exist.png").isEmpty());
        assertEquals(1, recursos.lee());
    }

    @Test
    void HU42_ca4_everyImagePackageInTheResourcesIsListedInAssetsMd() throws IOException {
        Path dirRecursos = Path.of("src/main/resources/assets");
        String listado = Files.readString(Path.of("docs/ASSETS.md"));

        assertTrue(listado.contains("Licencia"), "ASSETS.md explains the licences");
        if (!Files.isDirectory(dirRecursos)) {
            return;
        }
        try (Stream<Path> archivos = Files.walk(dirRecursos)) {
            List<Path> imagenes = archivos.filter(Files::isRegularFile)
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .toList();
            for (Path imagen : imagenes) {
                String relativo = dirRecursos.relativize(imagen).toString().replace('\\', '/');
                assertTrue(listado.contains(relativo) || listado.contains(relativo.split("/")[0] + "/"),
                        "not listed in docs/ASSETS.md: " + relativo);
            }
            assertFalse(imagenes.isEmpty() && !listado.contains("Ningún"), "ASSETS.md says when there are none");
        }
    }

    private static List<Integer> pixelesDe(BufferedImage imagen) {
        List<Integer> pixeles = new ArrayList<>();
        for (int y = 0; y < imagen.getHeight(); y += 7) {
            for (int x = 0; x < imagen.getWidth(); x += 7) {
                pixeles.add(imagen.getRGB(x, y));
            }
        }
        return pixeles;
    }
}
