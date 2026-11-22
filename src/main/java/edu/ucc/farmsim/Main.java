package edu.ucc.farmsim;

import edu.ucc.farmsim.application.controller.GameController;
import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.service.DayService;
import edu.ucc.farmsim.application.service.FarmService;
import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.application.service.ProgressService;
import edu.ucc.farmsim.application.service.SaveGameService;
import edu.ucc.farmsim.application.service.ViewMapper;
import edu.ucc.farmsim.domain.game.DayPipeline;
import edu.ucc.farmsim.domain.save.SaveRepository;
import edu.ucc.farmsim.infrastructure.persistence.JsonSaveRepository;
import edu.ucc.farmsim.ui.screens.MainWindow;
import edu.ucc.farmsim.ui.screens.ScreenRouter;
import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import javax.swing.SwingUtilities;

/**
 * Composition root: the only place that creates the services, the event bus, the repository and the window
 * and wires them together. The random seed of each new game comes from the clock.
 *
 * <p>Stories: HU-33.
 */
public final class Main {

    private static final int EXIT_NO_DISPLAY = 1;
    private static final String SAVE_FOLDER = ".farmsim";
    private static final String SAVE_SUBFOLDER = "saves";

    private Main() {
        // entry point only
    }

    /**
     * Starts the application.
     *
     * @param args command line arguments (unused)
     */
    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println(GameInfo.NAME + " necesita una pantalla para ejecutarse.");
            System.exit(EXIT_NO_DISPLAY);
            return;
        }
        EventBus bus = new EventBus();
        GameController controller = buildController(bus);
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow(GameInfo.NAME);
            window.setContentPane(new ScreenRouter(controller, bus, System::currentTimeMillis));
            window.setVisible(true);
        });
    }

    private static GameController buildController(EventBus bus) {
        GameSession session = new GameSession();
        ViewMapper mapper = new ViewMapper();
        Path saves = Path.of(System.getProperty("user.home"), SAVE_FOLDER, SAVE_SUBFOLDER);
        SaveRepository repository = new JsonSaveRepository(saves);
        return new GameController(session, mapper,
                new ProgressService(session, mapper, bus),
                new DayService(session, DayPipeline.standard(), mapper, bus),
                new SaveGameService(repository),
                new FarmService(session, mapper, bus));
    }
}
