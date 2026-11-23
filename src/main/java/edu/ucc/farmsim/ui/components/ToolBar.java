package edu.ucc.farmsim.ui.components;

import edu.ucc.farmsim.application.result.Names;
import edu.ucc.farmsim.ui.assets.AssetManager;
import edu.ucc.farmsim.ui.theme.Texts;
import edu.ucc.farmsim.ui.theme.WoodStyle;
import java.awt.FlowLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/**
 * The tool bar: one toggle button per tool (labelled with its key) and the seed chooser of the Sow tool.
 * It reads and writes the shared {@link ToolModel}, so the keys and the buttons always agree.
 *
 * <p>Stories: HU-36, HU-48.
 */
public final class ToolBar extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String[] SEED_CROPS = {"CARROT", "POTATO", "TOMATO", "CORN", "PUMPKIN"};
    private static final String SEED_SUFFIX = "_SEED";

    private final transient ToolModel model;
    private final JToggleButton[] buttons = new JToggleButton[Tool.values().length];
    private boolean updating;

    /**
     * Builds the bar.
     *
     * @param model the shared tool selection
     */
    public ToolBar(ToolModel model) {
        this(model, AssetManager.shared());
    }

    /**
     * Builds the bar with icons from the given assets (text only if they are missing).
     *
     * @param model  the shared tool selection
     * @param assets where the tool icons come from
     */
    public ToolBar(ToolModel model, AssetManager assets) {
        super(new FlowLayout(FlowLayout.CENTER, 6, 2));
        this.model = model;
        setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (Tool tool : Tool.values()) {
            JToggleButton button = new WoodSlot(tool.key() + " " + Texts.tool(tool));
            assets.icon("icons/" + tool.name() + ".png").ifPresent(button::setIcon);
            button.addActionListener(event -> {
                if (!updating) {
                    model.select(tool);
                }
            });
            buttons[tool.ordinal()] = button;
            group.add(button);
            add(button);
        }
        JLabel seedLabel = new JLabel(Texts.SEED_LABEL);
        seedLabel.setForeground(WoodStyle.INK);
        seedLabel.setFont(seedLabel.getFont().deriveFont(java.awt.Font.BOLD));
        add(seedLabel);
        String[] names = new String[SEED_CROPS.length];
        for (int i = 0; i < SEED_CROPS.length; i++) {
            names[i] = Names.crop(SEED_CROPS[i]);
        }
        JComboBox<String> seeds = new JComboBox<>(names);
        seeds.setFocusable(false);
        seeds.addActionListener(event -> {
            if (!updating) {
                model.selectSeed(SEED_CROPS[seeds.getSelectedIndex()] + SEED_SUFFIX);
            }
        });
        add(seeds);
        model.addChangeListener(this::syncFromModel);
        syncFromModel();
    }

    private void syncFromModel() {
        updating = true;
        try {
            Tool current = model.current();
            for (Tool tool : Tool.values()) {
                buttons[tool.ordinal()].setSelected(tool == current);
            }
            if (current == null) {
                // the button group keeps one selected; clear it explicitly
                for (JToggleButton button : buttons) {
                    button.getModel().setGroup(null);
                    button.setSelected(false);
                }
            }
        } finally {
            updating = false;
        }
    }
}
