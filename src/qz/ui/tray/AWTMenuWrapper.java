/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.ui.tray;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Wraps a Swing JMenuItem or JSeparator into an AWT item including text, shortcuts and action listeners.
 *
 * @author Tres Finocchiaro
 */
public class AWTMenuWrapper {
    private MenuItem item;

    private AWTMenuWrapper(JCheckBoxMenuItem item) {
        this.item = new CheckboxMenuItem(item.getText());
        wrapState(item);
        wrapEnabled(item);
        wrapShortcut(item);
        wrapItemListeners(item);
    }

    private AWTMenuWrapper(JMenuItem item) {
        this.item = new MenuItem(item.getText());
        wrapEnabled(item);
        wrapShortcut(item);
        wrapActionListeners(item);
    }

    private AWTMenuWrapper(JMenu menu) {
        this.item = new Menu(menu.getText());
        wrapEnabled(menu);
        wrapShortcut(menu);
    }

    @SuppressWarnings("unused")
    private AWTMenuWrapper(JSeparator ignore) {
        this.item = new MenuItem("-");
    }

    private MenuItem getMenuItem() {
        return item;
    }

    private void wrapShortcut(JMenuItem item) {
        this.item.setShortcut(new MenuShortcut(item.getMnemonic(), false));
    }

    private void wrapActionListeners(JMenuItem item) {
        for (ActionListener l : item.getActionListeners()) {
            this.item.addActionListener(l);
        }
    }

    // Special case for CheckboxMenuItem
    private void wrapItemListeners(final JMenuItem item) {
        for (final ActionListener l : item.getActionListeners()) {
            ((CheckboxMenuItem)this.item).addItemListener(e -> {
                ((JCheckBoxMenuItem)item).setState(((CheckboxMenuItem)AWTMenuWrapper.this.item).getState());
                l.actionPerformed(new ActionEvent(item, e.getID(), item.getActionCommand()));
            });
        }
    }

    private void wrapState(JMenuItem item) {
        if (this.item instanceof CheckboxMenuItem && item instanceof JCheckBoxMenuItem) {
            // Match initial state
            ((CheckboxMenuItem)this.item).setState(((JCheckBoxMenuItem)item).getState());
            // Monitor future state
            item.addChangeListener(e -> ((CheckboxMenuItem)AWTMenuWrapper.this.item).setState(((JCheckBoxMenuItem)item).getState()));
        }
    }

    private void wrapEnabled(JMenuItem item) {
        // Match initial state
        this.item.setEnabled(item.isEnabled());
        // Monitor future state
        item.addPropertyChangeListener("enabled", evt -> {
            this.item.setEnabled((Boolean)evt.getNewValue());
        });
    }

    public static MenuItem wrap(Component c) {
        if (c instanceof JCheckBoxMenuItem) {
            return new AWTMenuWrapper((JCheckBoxMenuItem)c).getMenuItem();
        } else if (c instanceof JMenu) {
            return new AWTMenuWrapper((JMenu)c).getMenuItem();
        } else if (c instanceof JSeparator) {
            return new AWTMenuWrapper((JSeparator)c).getMenuItem();
        } else if (c instanceof JMenuItem) {
            return new AWTMenuWrapper((JMenuItem)c).getMenuItem();
        } else {
            return new MenuItem("Error");
        }
    }
}