package qz.ui;

import javax.swing.*;
import java.awt.*;

public class ThemeUtilities {
    public static void refreshAll(Container container) {
        refreshAll(container, null);
    }
    public static void refreshAll(Container container, Component ... stragglers) {
        refreshAll(container.getComponents());

        // Handle orphaned UI objects (e.g. Component added to a message dialog)
        if (stragglers != null) {
            for(Component straggler : stragglers) {
                Container parent = getRootPane(straggler);
                if (parent != null) {
                    SwingUtilities.updateComponentTreeUI(parent);
                }
            }
            refreshAll(stragglers);
        }
    }
    private static void refreshAll(Component ... components) {
        if (components != null) {
            for(Component c : components) {
                if (c instanceof Themeable) {
                    ((Themeable)c).refresh();
                }
                if (c instanceof Container) {
                    refreshAll((Container)c);
                }
            }
        }
    }

    private static Container getRootPane(Component c) {
        if (c != null) {
            if (c instanceof JRootPane) {
                return (Container)c;
            }
            return getRootPane(c.getParent());
        }
        return null;
    }
}
