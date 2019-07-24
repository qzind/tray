package qz.ui;

import org.apache.commons.lang3.ArrayUtils;

import javax.swing.*;
import java.awt.*;

public class ThemeUtilities {
    public static void refreshAll(Container container, Component ... orphans) {
        // Handle orphaned UI objects (e.g. Component added to a message dialog)
        for(Component orphan : orphans) {
            recurseOrphanedComponents(orphan);
        }
        refreshAll(ArrayUtils.addAll(container.getComponents(), orphans));
    }

    private static void refreshAll(Component ... components) {
        for(Component c : components) {
            if (c instanceof Themeable) {
                ((Themeable)c).refresh();
            }
            if (c instanceof Container) {
                refreshAll((Container)c);
            }
        }
    }

    /**
     * Inefficient yet effective way to recurse orphaned component's UI changes
     */
    private static Container recurseOrphanedComponents(Component c) {
        if (c != null) {
            SwingUtilities.updateComponentTreeUI(c);
            if (c instanceof JRootPane) {
                return (Container)c;
            }
            return recurseOrphanedComponents(c.getParent());
        }
        return null;
    }
}
