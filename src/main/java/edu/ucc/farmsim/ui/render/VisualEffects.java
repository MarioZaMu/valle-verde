package edu.ucc.farmsim.ui.render;

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
 * Everything that moves or changes colour on the map and is not the game: the palette of the season, the rain
 * and the snow, the night tint of the end-of-day transition, the frames of the animals and the growth of a crop
 * that changes stage (HU-40, HU-41). It only receives what the screen already knows (the season and weather of
 * the events, the map views) and never calls the game, so switching it off cannot change the state (RN-08,
 * RN-25). It takes its clock from outside and has no Swing, so it is tested without waiting.
 *
 * <p>Stories: HU-40, HU-41.
 */
public final class VisualEffects {

    /** Milliseconds the night tint lasts after a day ends. */
    public static final int TRANSITION_MS = 900;

    /** Milliseconds a crop takes to grow into its new stage (HU-41 allows 500). */
    public static final int GROWTH_MS = 400;

    /** Milliseconds each animal frame is shown. */
    public static final int FRAME_MS = 250;

    /** Frames of the animal animation. */
    public static final int ANIMAL_FRAMES = 3;

    /** Most particles on screen, so a frame stays cheap. */
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

    /**
     * Creates the effects.
     *
     * @param clock  current time in milliseconds
     * @param random where the particles start
     */
    public VisualEffects(LongSupplier clock, Random random) {
        this.clock = clock;
        this.random = random;
    }

    /** Whether the effects and animations are on. */
    public boolean isEnabled() {
        return enabled;
    }

    /** Switches them on or off; off, the map is drawn plainly and nothing moves. */
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

    /** The season id and weather id the world is in now. */
    public void onWorld(String newSeason, String newWeather) {
        this.season = newSeason;
        this.weather = newWeather;
        rebuildParticles();
    }

    /** A game started or was loaded: forget what the previous one looked like. */
    public void reset() {
        stages.clear();
        grownAt.clear();
        seenMap = false;
        dayEndedAt = -1;
    }

    /** The state already advanced to the next day; the night tint starts now. */
    public void onDayEnded() {
        dayEndedAt = clock.getAsLong();
    }

    /** The map as the game has it now: a crop whose stage changed starts its growth animation. */
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
    }

    /** The season id shown. */
    public String season() {
        return season;
    }

    /** The weather id shown. */
    public String weather() {
        return weather;
    }

    /** How dark the night tint is now, from 0 to 1; 0 when off or outside the transition. */
    public float nightAlpha() {
        if (!enabled || dayEndedAt < 0) {
            return 0f;
        }
        double progress = (clock.getAsLong() - dayEndedAt) / (double) TRANSITION_MS;
        return progress >= 1.0 || progress < 0 ? 0f : (float) Math.sin(Math.PI * progress);
    }

    /** The particles to draw now. */
    public List<Particle> particles() {
        return Collections.unmodifiableList(particles);
    }

    /** Moves the particles according to the time since the last step. */
    public void step() {
        long now = clock.getAsLong();
        double seconds = lastStep < 0 ? FRAME_MS / 1000.0 / 8 : Math.min(MAX_STEP_SECONDS, (now - lastStep) / 1000.0);
        lastStep = now;
        for (Particle particle : particles) {
            particle.advance(seconds);
        }
    }

    /** Which frame, from 0 to {@link #ANIMAL_FRAMES} - 1, the animal at the given place of its building shows. */
    public int animalFrame(int animalIndex) {
        if (!enabled) {
            return 0;
        }
        return (int) (((clock.getAsLong() / FRAME_MS) + animalIndex) % ANIMAL_FRAMES);
    }

    /** Size of the crop at a plot, from about one half up to 1 while it grows into its new stage. */
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

    /** True while something is moving, so the view needs to keep drawing frames. */
    public boolean needsFrames() {
        if (!enabled) {
            return false;
        }
        long now = clock.getAsLong();
        boolean growing = grownAt.values().stream().anyMatch(start -> now - start < GROWTH_MS);
        return !particles.isEmpty() || nightAlpha() > 0f || growing || animals;
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
