package edu.ucc.farmsim.interfaz.dibujo;

import edu.ucc.farmsim.aplicacion.dto.VistaAvatar;
import edu.ucc.farmsim.aplicacion.dto.VistaMapa;
import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.LongSupplier;

/**
 * Todo lo que se mueve o cambia de color en el mapa y no es el juego: la paleta de la estación, la lluvia y la nieve,
 * el tinte de noche de la transición de fin de día, los cuadros de los animales y el crecimiento de un cultivo que
 * cambia de etapa (HU-40, HU-41) y la caminata del avatar entre dos parcelas (HU-45). Solo recibe lo que la pantalla ya
 * sabe (la estación y el clima de los eventos, las vistas del mapa) y nunca llama al juego, así que apagarlo no puede
 * cambiar el estado (RN-08, RN-25). Toma su reloj de afuera y no tiene Swing, así que se prueba sin esperar.
 *
 * <p>Stories: HU-40, HU-41, HU-45, HU-48, HU-50.
 */
public final class EfectosVisuales {

    /** Milisegundos que dura el tinte de noche después de que termina un día. */
    public static final int MS_TRANSICION = 900;

    /** Milisegundos que tarda un cultivo en crecer hasta su nueva etapa (HU-41 permite 500). */
    public static final int MS_CRECIMIENTO = 400;

    /** Milisegundos que se muestra cada cuadro de animal. */
    public static final int MS_MARCO = 250;

    /** Cuadros de la animación de los animales. */
    public static final int MARCOS_ANIMAL = 3;

    /** Cuánto tarda el avatar en caminar de una parcela a la siguiente, en milisegundos (HU-45 · CA-5). */
    public static final int MS_PASO = 180;

    /** Cuadros de la caminata del avatar; en reposo muestra el primero. */
    public static final int MARCOS_AVATAR = 3;

    /** Máximo de partículas en pantalla, para que un cuadro siga siendo barato. */
    public static final int PARTICULAS_MAXIMOS = 120;

    private static final int CONTEO_LLUVIA = 110;
    private static final int CONTEO_NIEVE = 80;
    private static final double SEGUNDOS_PASO_MAXIMOS = 0.1;
    private static final float ESCALA_INICIO = 0.5f;

    private final LongSupplier reloj;
    private final Random azar;
    private final List<Particula> particulas = new ArrayList<>();
    private final Map<Integer, String> etapas = new HashMap<>();
    private final Map<Integer, Long> crecidoEn = new HashMap<>();
    private boolean habilitado = true;
    private String estacion = "SPRING";
    private String clima = "SUNNY";
    private long diaTerminadoEn = -1;
    private long pasoUltimo = -1;
    private boolean mapaVisto;
    private boolean animales;
    private boolean ambiente;
    private int[] avatarEn;
    private int[] avatarDesde;
    private long inicioPaso = -1;
    private long desdeCaminando = -1;

    /**
     * Crea los efectos.
     *
     * @param reloj  hora actual en milisegundos
     * @param azar de donde parten las partículas
     */
    public EfectosVisuales(LongSupplier reloj, Random azar) {
        this.reloj = reloj;
        this.azar = azar;
    }

    /** Si los efectos y las animaciones están activados. */
    public boolean isEnabled() {
        return habilitado;
    }

    /** Los activa o desactiva; desactivados, el mapa se dibuja simple y nada se mueve. */
    public void setEnabled(boolean on) {
        this.habilitado = on;
        if (on) {
            particulasReconstruir();
        } else {
            particulas.clear();
            diaTerminadoEn = -1;
            crecidoEn.clear();
        }
    }

    /**
     * Activa o desactiva la vida ambiental: agua que ondea, árboles que se mecen con el viento, animales que deambulan,
     * nubes, mariposas y hojas (HU-48). Activada, la vista sigue dibujando cuadros mientras se muestra.
     */
    public void ambienteFijar(boolean on) {
        this.ambiente = on;
    }

    /** Verdadero cuando la vida ambiental está activada y los efectos están habilitados. */
    public boolean esAmbiente() {
        return ambiente && habilitado;
    }

    /** La hora actual del reloj de los efectos, en milisegundos. */
    public long tiempo() {
        return reloj.getAsLong();
    }

    /** El id de estación y el id de clima en que está el mundo ahora. */
    public void onMundo(String estacionNueva, String climaNuevo) {
        this.estacion = estacionNueva;
        this.clima = climaNuevo;
        particulasReconstruir();
    }

    /** Empezó o se cargó una partida: olvida cómo se veía la anterior. */
    public void reiniciar() {
        etapas.clear();
        crecidoEn.clear();
        mapaVisto = false;
        diaTerminadoEn = -1;
        avatarEn = null;
        avatarDesde = null;
        inicioPaso = -1;
    }

    /** El estado ya avanzó al día siguiente; el tinte de noche empieza ahora. */
    public void onDiaTerminado() {
        diaTerminadoEn = reloj.getAsLong();
    }

    /** El mapa tal como lo tiene ahora la partida: un cultivo cuya etapa cambió empieza su animación de crecimiento. */
    public void onMapa(VistaMapa map) {
        long now = reloj.getAsLong();
        for (VistaParcela parcela : map.parcelas()) {
            int clave = parcela.fila() * 1000 + parcela.col();
            String etapa = parcela.etapa();
            String antes = etapas.get(clave);
            if (mapaVisto && etapa != null && antes != null && !antes.equals(etapa)) {
                crecidoEn.put(clave, now);
            }
            if (etapa == null) {
                etapas.remove(clave);
                crecidoEn.remove(clave);
            } else {
                etapas.put(clave, etapa);
            }
        }
        mapaVisto = true;
        animales = map.edificios().stream().anyMatch(b -> !b.animales().isEmpty());
        onAvatar(map.avatar(), now);
    }

    /** Un paso a una parcela vecina empieza la caminata; un salto (una partida cargada) solo pone al avatar allí. */
    private void onAvatar(VistaAvatar avatar, long now) {
        if (avatar == null) {
            avatarEn = null;
            return;
        }
        int[] aqui = {avatar.col(), avatar.fila()};
        if (avatarEn != null && Math.abs(aqui[0] - avatarEn[0]) + Math.abs(aqui[1] - avatarEn[1]) == 1) {
            if (inicioPaso < 0 || now - inicioPaso > MS_PASO * 2L) {
                desdeCaminando = now;
            }
            avatarDesde = avatarEn;
            inicioPaso = now;
        }
        avatarEn = aqui;
    }

    /** Verdadero mientras el avatar camina de una parcela a la siguiente. */
    public boolean esCaminando() {
        return habilitado && inicioPaso >= 0 && reloj.getAsLong() - inicioPaso < MS_PASO;
    }

    /** Qué cuadro de la caminata muestra el avatar: uno de {@link #MARCOS_AVATAR} mientras camina, 0 en reposo. */
    public int marcoAvatar() {
        if (!esCaminando()) {
            return 0;
        }
        long caminado = reloj.getAsLong() - desdeCaminando;
        return (int) ((caminado * MARCOS_AVATAR / MS_PASO) % MARCOS_AVATAR);
    }

    /** Cuánto ha avanzado el avatar en el camino desde la parcela que dejó, de 0 (todavía allí) a 1 (llegó). */
    public double progresoAvatar() {
        return esCaminando() ? (reloj.getAsLong() - inicioPaso) / (double) MS_PASO : 1.0;
    }

    /**
     * Dónde se dibuja ahora el avatar, en parcelas: entre la parcela que dejó y a la que va mientras camina, sobre su
     * parcela en reposo; {@code null} sin avatar. La cámara sigue este punto (RN-40).
     */
    public double[] posicionAvatar() {
        if (avatarEn == null) {
            return null;
        }
        if (!esCaminando() || avatarDesde == null) {
            return new double[] {avatarEn[0], avatarEn[1]};
        }
        double t = progresoAvatar();
        return new double[] {avatarDesde[0] + (avatarEn[0] - avatarDesde[0]) * t,
            avatarDesde[1] + (avatarEn[1] - avatarDesde[1]) * t};
    }

    /** La parcela {columna, fila} desde la que camina el avatar, o {@code null} en reposo. */
    public int[] avatarDesde() {
        return esCaminando() ? avatarDesde.clone() : null;
    }

    /** El id de la estación que se muestra. */
    public String estacion() {
        return estacion;
    }

    /** El id del clima que se muestra. */
    public String clima() {
        return clima;
    }

    /** Qué tan oscuro es ahora el tinte de noche, de 0 a 1; 0 cuando está apagado o fuera de la transición. */
    public float alfaNoche() {
        if (!habilitado || diaTerminadoEn < 0) {
            return 0f;
        }
        double progreso = (reloj.getAsLong() - diaTerminadoEn) / (double) MS_TRANSICION;
        return progreso >= 1.0 || progreso < 0 ? 0f : (float) Math.sin(Math.PI * progreso);
    }

    /** Las partículas que se dibujan ahora. */
    public List<Particula> particulas() {
        return Collections.unmodifiableList(particulas);
    }

    /** Mueve las partículas según el tiempo desde el último paso. */
    public void paso() {
        long now = reloj.getAsLong();
        double segundos = pasoUltimo < 0 ? MS_MARCO / 1000.0 / 8 : Math.min(SEGUNDOS_PASO_MAXIMOS, (now - pasoUltimo) / 1000.0);
        pasoUltimo = now;
        for (Particula particula : particulas) {
            particula.avanzar(segundos);
        }
    }

    /** Qué cuadro, de 0 a {@link #MARCOS_ANIMAL} - 1, muestra el animal del lugar dado de su edificio. */
    public int marcoAnimal(int indiceAnimal) {
        if (!habilitado) {
            return 0;
        }
        return (int) (((reloj.getAsLong() / MS_MARCO) + indiceAnimal) % MARCOS_ANIMAL);
    }

    /** Tamaño del cultivo en una parcela, de aproximadamente la mitad hasta 1 mientras crece hasta su nueva etapa. */
    public float escalaCultivo(int col, int fila) {
        Long start = crecidoEn.get(fila * 1000 + col);
        if (!habilitado || start == null) {
            return 1f;
        }
        double progreso = (reloj.getAsLong() - start) / (double) MS_CRECIMIENTO;
        if (progreso >= 1.0) {
            return 1f;
        }
        double suavizado = 1 - Math.pow(1 - Math.max(0, progreso), 2);
        return (float) (ESCALA_INICIO + (1 - ESCALA_INICIO) * suavizado);
    }

    /** Verdadero mientras algo se mueve, de modo que la vista necesita seguir dibujando cuadros. */
    public boolean marcosNecesita() {
        if (!habilitado) {
            return false;
        }
        long now = reloj.getAsLong();
        boolean creciendo = crecidoEn.values().stream().anyMatch(start -> now - start < MS_CRECIMIENTO);
        return ambiente || !particulas.isEmpty() || alfaNoche() > 0f || creciendo || animales || esCaminando();
    }

    private void particulasReconstruir() {
        particulas.clear();
        if (!habilitado) {
            return;
        }
        if ("RAINY".equals(clima)) {
            for (int i = 0; i < Math.min(CONTEO_LLUVIA, PARTICULAS_MAXIMOS); i++) {
                particulas.add(new Particula(Particula.Categoria.RAIN, azar.nextDouble(), azar.nextDouble(),
                        0.9 + azar.nextDouble() * 0.5, -0.05));
            }
        } else if ("SNOWY".equals(clima)) {
            for (int i = 0; i < Math.min(CONTEO_NIEVE, PARTICULAS_MAXIMOS); i++) {
                particulas.add(new Particula(Particula.Categoria.SNOW, azar.nextDouble(), azar.nextDouble(),
                        0.08 + azar.nextDouble() * 0.12, (azar.nextDouble() - 0.5) * 0.06));
            }
        }
    }
}
