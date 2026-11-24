package edu.ucc.farmsim.ui.render;

import edu.ucc.farmsim.application.dto.AvatarView;
import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.PlotView;
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
public final class VisualEffects {

    /** Milisegundos que dura el tinte de noche después de que termina un día. */
    public static final int TRANSITION_MS = 900;

    /** Milisegundos que tarda un cultivo en crecer hasta su nueva etapa (HU-41 permite 500). */
    public static final int GROWTH_MS = 400;

    /** Milisegundos que se muestra cada cuadro de animal. */
    public static final int FRAME_MS = 250;

    /** Cuadros de la animación de los animales. */
    public static final int ANIMAL_FRAMES = 3;

    /** Cuánto tarda el avatar en caminar de una parcela a la siguiente, en milisegundos (HU-45 · CA-5). */
    public static final int STEP_MS = 180;

    /** Cuadros de la caminata del avatar; en reposo muestra el primero. */
    public static final int AVATAR_FRAMES = 3;

    /** Máximo de partículas en pantalla, para que un cuadro siga siendo barato. */
    public static final int MAX_PARTICLES = 120;

    private static final int RAIN_COUNT = 110;
    private static final int SNOW_COUNT = 80;
    private static final double MAX_STEP_SECONDS = 0.1;
    private static final float START_SCALE = 0.5f;

    private final LongSupplier clock;
    private final Random random;
    private final List<Particle> particles = new ArrayList<>();
    private final Map<Integer, String> stages = new HashMap<>();
    private final Map<Integer, Long> grownAt = new HashMap<>();
    private boolean enabled = true;
    private String season = "SPRING";
    private String weather = "SUNNY";
    private long dayEndedAt = -1;
    private long lastStep = -1;
    private boolean seenMap;
    private boolean animals;
    private boolean ambient;
    private int[] avatarAt;
    private int[] avatarFrom;
    private long stepStart = -1;
    private long walkingSince = -1;

    /**
     * Crea los efectos.
     *
     * @param clock  hora actual en milisegundos
     * @param random de donde parten las partículas
     */
    public VisualEffects(LongSupplier clock, Random random) {
        this.clock = clock;
        this.random = random;
    }

    /** Si los efectos y las animaciones están activados. */
    public boolean isEnabled() {
        return enabled;
    }

    /** Los activa o desactiva; desactivados, el mapa se dibuja simple y nada se mueve. */
    public void setEnabled(boolean on) {
        this.enabled = on;
        if (on) {
            rebuildParticles();
        } else {
            particles.clear();
            dayEndedAt = -1;
            grownAt.clear();
        }
    }

    /**
     * Activa o desactiva la vida ambiental: agua que ondea, árboles que se mecen con el viento, animales que deambulan,
     * nubes, mariposas y hojas (HU-48). Activada, la vista sigue dibujando cuadros mientras se muestra.
     */
    public void setAmbient(boolean on) {
        this.ambient = on;
    }

    /** Verdadero cuando la vida ambiental está activada y los efectos están habilitados. */
    public boolean isAmbient() {
        return ambient && enabled;
    }

    /** La hora actual del reloj de los efectos, en milisegundos. */
    public long time() {
        return clock.getAsLong();
    }

    /** El id de estación y el id de clima en que está el mundo ahora. */
    public void onWorld(String newSeason, String newWeather) {
        this.season = newSeason;
        this.weather = newWeather;
        rebuildParticles();
    }

    /** Empezó o se cargó una partida: olvida cómo se veía la anterior. */
    public void reset() {
        stages.clear();
        grownAt.clear();
        seenMap = false;
        dayEndedAt = -1;
        avatarAt = null;
        avatarFrom = null;
        stepStart = -1;
    }

    /** El estado ya avanzó al día siguiente; el tinte de noche empieza ahora. */
    public void onDayEnded() {
        dayEndedAt = clock.getAsLong();
    }

    /** El mapa tal como lo tiene ahora la partida: un cultivo cuya etapa cambió empieza su animación de crecimiento. */
    public void onMap(MapView map) {
        long now = clock.getAsLong();
        for (PlotView plot : map.plots()) {
            int key = plot.row() * 1000 + plot.col();
            String stage = plot.stage();
            String before = stages.get(key);
            if (seenMap && stage != null && before != null && !before.equals(stage)) {
                grownAt.put(key, now);
            }
            if (stage == null) {
                stages.remove(key);
                grownAt.remove(key);
            } else {
                stages.put(key, stage);
            }
        }
        seenMap = true;
        animals = map.buildings().stream().anyMatch(b -> !b.animals().isEmpty());
        onAvatar(map.avatar(), now);
    }

    /** Un paso a una parcela vecina empieza la caminata; un salto (una partida cargada) solo pone al avatar allí. */
    private void onAvatar(AvatarView avatar, long now) {
        if (avatar == null) {
            avatarAt = null;
            return;
        }
        int[] here = {avatar.col(), avatar.row()};
        if (avatarAt != null && Math.abs(here[0] - avatarAt[0]) + Math.abs(here[1] - avatarAt[1]) == 1) {
            if (stepStart < 0 || now - stepStart > STEP_MS * 2L) {
                walkingSince = now;
            }
            avatarFrom = avatarAt;
            stepStart = now;
        }
        avatarAt = here;
    }

    /** Verdadero mientras el avatar camina de una parcela a la siguiente. */
    public boolean isWalking() {
        return enabled && stepStart >= 0 && clock.getAsLong() - stepStart < STEP_MS;
    }

    /** Qué cuadro de la caminata muestra el avatar: uno de {@link #AVATAR_FRAMES} mientras camina, 0 en reposo. */
    public int avatarFrame() {
        if (!isWalking()) {
            return 0;
        }
        long walked = clock.getAsLong() - walkingSince;
        return (int) ((walked * AVATAR_FRAMES / STEP_MS) % AVATAR_FRAMES);
    }

    /** Cuánto ha avanzado el avatar en el camino desde la parcela que dejó, de 0 (todavía allí) a 1 (llegó). */
    public double avatarProgress() {
        return isWalking() ? (clock.getAsLong() - stepStart) / (double) STEP_MS : 1.0;
    }

    /**
     * Dónde se dibuja ahora el avatar, en parcelas: entre la parcela que dejó y a la que va mientras camina, sobre su
     * parcela en reposo; {@code null} sin avatar. La cámara sigue este punto (RN-40).
     */
    public double[] avatarPosition() {
        if (avatarAt == null) {
            return null;
        }
        if (!isWalking() || avatarFrom == null) {
            return new double[] {avatarAt[0], avatarAt[1]};
        }
        double t = avatarProgress();
        return new double[] {avatarFrom[0] + (avatarAt[0] - avatarFrom[0]) * t,
            avatarFrom[1] + (avatarAt[1] - avatarFrom[1]) * t};
    }

    /** La parcela {columna, fila} desde la que camina el avatar, o {@code null} en reposo. */
    public int[] avatarFrom() {
        return isWalking() ? avatarFrom.clone() : null;
    }

    /** El id de la estación que se muestra. */
    public String season() {
        return season;
    }

    /** El id del clima que se muestra. */
    public String weather() {
        return weather;
    }

    /** Qué tan oscuro es ahora el tinte de noche, de 0 a 1; 0 cuando está apagado o fuera de la transición. */
    public float nightAlpha() {
        if (!enabled || dayEndedAt < 0) {
            return 0f;
        }
        double progress = (clock.getAsLong() - dayEndedAt) / (double) TRANSITION_MS;
        return progress >= 1.0 || progress < 0 ? 0f : (float) Math.sin(Math.PI * progress);
    }

    /** Las partículas que se dibujan ahora. */
    public List<Particle> particles() {
        return Collections.unmodifiableList(particles);
    }

    /** Mueve las partículas según el tiempo desde el último paso. */
    public void step() {
        long now = clock.getAsLong();
        double seconds = lastStep < 0 ? FRAME_MS / 1000.0 / 8 : Math.min(MAX_STEP_SECONDS, (now - lastStep) / 1000.0);
        lastStep = now;
        for (Particle particle : particles) {
            particle.advance(seconds);
        }
    }

    /** Qué cuadro, de 0 a {@link #ANIMAL_FRAMES} - 1, muestra el animal del lugar dado de su edificio. */
    public int animalFrame(int animalIndex) {
        if (!enabled) {
            return 0;
        }
        return (int) (((clock.getAsLong() / FRAME_MS) + animalIndex) % ANIMAL_FRAMES);
    }

    /** Tamaño del cultivo en una parcela, de aproximadamente la mitad hasta 1 mientras crece hasta su nueva etapa. */
    public float cropScale(int col, int row) {
        Long start = grownAt.get(row * 1000 + col);
        if (!enabled || start == null) {
            return 1f;
        }
        double progress = (clock.getAsLong() - start) / (double) GROWTH_MS;
        if (progress >= 1.0) {
            return 1f;
        }
        double eased = 1 - Math.pow(1 - Math.max(0, progress), 2);
        return (float) (START_SCALE + (1 - START_SCALE) * eased);
    }

    /** Verdadero mientras algo se mueve, de modo que la vista necesita seguir dibujando cuadros. */
    public boolean needsFrames() {
        if (!enabled) {
            return false;
        }
        long now = clock.getAsLong();
        boolean growing = grownAt.values().stream().anyMatch(start -> now - start < GROWTH_MS);
        return ambient || !particles.isEmpty() || nightAlpha() > 0f || growing || animals || isWalking();
    }

    private void rebuildParticles() {
        particles.clear();
        if (!enabled) {
            return;
        }
        if ("RAINY".equals(weather)) {
            for (int i = 0; i < Math.min(RAIN_COUNT, MAX_PARTICLES); i++) {
                particles.add(new Particle(Particle.Kind.RAIN, random.nextDouble(), random.nextDouble(),
                        0.9 + random.nextDouble() * 0.5, -0.05));
            }
        } else if ("SNOWY".equals(weather)) {
            for (int i = 0; i < Math.min(SNOW_COUNT, MAX_PARTICLES); i++) {
                particles.add(new Particle(Particle.Kind.SNOW, random.nextDouble(), random.nextDouble(),
                        0.08 + random.nextDouble() * 0.12, (random.nextDouble() - 0.5) * 0.06));
            }
        }
    }
}
