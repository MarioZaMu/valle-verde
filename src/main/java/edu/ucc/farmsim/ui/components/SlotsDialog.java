package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.ui.theme.Texts;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.function.IntConsumer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;

/**
 * Las ranuras de guardado: las tres con lo que contienen, y los botones Guardar y Cargar (HU-29, HU-30, HU-32). Guardar
 * en una ranura ocupada pregunta primero. Es modal; {@code Esc} lo cierra. Toda la lógica está en {@link
 * SlotsPresenter}.
 *
 * <p>Stories: HU-29, HU-30, HU-32.
 */
public final class SlotsDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final transient SlotsPresenter presenter;
    private final transient IntConsumer onUsed;
    private final transient Runnable onLoaded;
    private final DefaultListModel<String> items = new DefaultListModel<>();
    private final JList<String> list = new JList<>(items);
    private final JButton save = new JButton(Texts.SAVE);
    private final JButton load = new JButton(Texts.LOAD);
    private final JLabel message = new JLabel(" ");

    /**
     * Construye el diálogo.
     *
     * @param owner     la ventana a la que pertenece
     * @param presenter la lógica de las ranuras
     * @param onUsed    se le avisa qué ranura se guardó o se cargó, para que las teclas rápidas lo sepan
     * @param onLoaded  se le avisa cuando se cargó una partida (el diálogo se cierra solo después)
     */
    public SlotsDialog(Window owner, SlotsPresenter presenter, IntConsumer onUsed, Runnable onLoaded) {
        super(owner, Texts.SLOTS_TITLE, ModalityType.APPLICATION_MODAL);
        this.presenter = presenter;
        this.onUsed = onUsed;
        this.onLoaded = onLoaded;
        list.setName("slot-list");
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(event -> refreshButtons());
        save.setName("save-button");
        load.setName("load-button");
        save.addActionListener(event -> onSave());
        load.addActionListener(event -> onLoad());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
        south.add(save);
        south.add(load);
        south.add(message);
        setLayout(new BorderLayout());
        add(list, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        getRootPane().registerKeyboardAction(event -> dispose(), KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        reload();
        setSize(560, 220);
        setLocationRelativeTo(owner);
    }

    private int selectedSlot() {
        return list.getSelectedIndex() + 1;
    }

    private void onSave() {
        int slot = selectedSlot();
        if (slot < 1) {
            return;
        }
        String text = presenter.save(slot, () -> JOptionPane.showConfirmDialog(this, Texts.CONFIRM_OVERWRITE,
                Texts.SLOTS_TITLE, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION);
        if (!text.isEmpty()) {
            message.setText(text);
            onUsed.accept(slot);
        }
        reload();
    }

    private void onLoad() {
        int slot = selectedSlot();
        if (slot < 1) {
            return;
        }
        var result = presenter.load(slot);
        if (result.isOk()) {
            onUsed.accept(slot);
            onLoaded.run();
            dispose();
        } else {
            message.setText(result.message());
            reload();
        }
    }

    private void reload() {
        int selected = list.getSelectedIndex();
        items.clear();
        presenter.lines().forEach(items::addElement);
        if (!presenter.error().isEmpty()) {
            message.setText(presenter.error());
        }
        if (selected >= 0 && selected < items.size()) {
            list.setSelectedIndex(selected);
        }
        refreshButtons();
    }

    private void refreshButtons() {
        int slot = selectedSlot();
        save.setEnabled(slot >= 1);
        load.setEnabled(slot >= 1 && presenter.canLoad(slot));
    }
}
