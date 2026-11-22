package edu.ucc.farmsim.ui.components;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * A panel that stacks components on top of each other, every one covering the whole area. The first one added
 * is the one on top; its size is the one of the base component (the last one added).
 *
 * <p>Stories: HU-39.
 */
public final class OverlayPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient JComponent base;

    /**
     * Creates the panel around its base component.
     *
     * @param base the component that sets the size and sits at the bottom
     */
    public OverlayPanel(JComponent base) {
        super(null);
        this.base = base;
        add(base);
    }

    /** Puts a component above everything added before. */
    public void addOnTop(JComponent overlay) {
        add(overlay, 0);
    }

    @Override
    public Dimension getPreferredSize() {
        return base.getPreferredSize();
    }

    @Override
    public void doLayout() {
        for (Component child : getComponents()) {
            child.setBounds(0, 0, getWidth(), getHeight());
        }
    }
}
