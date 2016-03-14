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

import org.jdesktop.swinghelper.tray.JXTrayIcon;

import javax.swing.*;
import java.awt.*;

/**
 * Wraps a Swing JPopupMenu into an AWT PopupMenu
 *
 * @author Tres Finocchiaro
 */
public class ClassicTrayIcon extends JXTrayIcon {
    public ClassicTrayIcon(Image image) {
        super(image);
    }

    @Override
    public void setJPopupMenu(JPopupMenu source) {
        final PopupMenu popup = new PopupMenu();
        setPopupMenu(popup);
        wrapAll(popup, source.getComponents());
    }

    /**
     * Convert an array of Swing menu components to its AWT equivalent
     * @param menu PopupMenu to receive new components
     * @param components Array of components to recurse over
     */
    private static void wrapAll(Menu menu, Component[] components) {
        for (Component c : components) {
            MenuItem item = AWTMenuWrapper.wrap(c);
            menu.add(item);
            if (item instanceof Menu) {
                wrapAll((Menu)item, ((JMenu)c).getMenuComponents());
            }
        }
    }
}