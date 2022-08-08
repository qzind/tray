/*
 * Copyright 2008 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.jdesktop.swinghelper.tray;

import com.github.zafarkhaja.semver.Version;
import qz.common.Constants;
import qz.utils.MacUtilities;
import qz.utils.SystemUtilities;
import qz.utils.WindowsUtilities;

import javax.swing.*;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.PopupMenuEvent;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class JXTrayIcon extends TrayIcon {
    private JPopupMenu menu;
    private static JDialog dialog;
    static {
        dialog = new JDialog((Frame) null);
        dialog.setUndecorated(true);
        dialog.setAlwaysOnTop(true);
    }

    private static PopupMenuListener popupListener = new PopupMenuListener() {
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        }

        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            dialog.setVisible(false);
        }

        public void popupMenuCanceled(PopupMenuEvent e) {
            dialog.setVisible(false);
        }
    };


    public JXTrayIcon(Image image) {
        super(image);
        addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                showJPopupMenu(e);
            }
        });
    }

    protected void showJPopupMenu(MouseEvent mouseEvent) {
        if (menu != null) {
            Point location = mouseEvent.getLocationOnScreen();
            // Handle HiDPI factor discrepancy between mouse position and window position
            if(SystemUtilities.isWindows() && Constants.JAVA_VERSION.getMajorVersion() >= 9) {
                location.setLocation(location.getX() / WindowsUtilities.getScaleFactor(), location.getY() / WindowsUtilities.getScaleFactor());
            }
            showJPopupMenu((int)location.getX(), (int)location.getY());
        }
    }

    protected void showJPopupMenu(int x, int y) {
        dialog.setLocation(x, y);
        dialog.setVisible(true);

        // Show the menu centered on the invisible dialog
        menu.show(dialog.getContentPane(), 0, 0);

        // Compensate position variance due to off-screen placement
        Dimension menuSize = menu.getPreferredSize();
        Point menuLocation = menu.getLocationOnScreen();
        int x2 = x; int y2 = y;
        if (menuLocation.getY() > y) y2 = y + menuSize.height;
        if (menuLocation.getY() < y) y2 = y - menuSize.height;
        if (menuLocation.getX() > x) x2 = x + menuSize.width;
        if (menuLocation.getX() < x) x2 = x - menuSize.width;
        if(x2 != x || y2 != y) {
            menu.setLocation(x2, y2);
        }

        // popup works only for focused windows
        dialog.toFront();
    }

    public JPopupMenu getJPopupMenu() {
        return menu;
    }

    public void setJPopupMenu(JPopupMenu menu) {
        if (this.menu != null) {
            this.menu.removePopupMenuListener(popupListener);
        }
        this.menu = menu;
        menu.addPopupMenuListener(popupListener);
    }

    private static void createGui() {
        JXTrayIcon tray = new JXTrayIcon(createImage());
        tray.setJPopupMenu(createJPopupMenu());
        try {
            SystemTray.getSystemTray().add(tray);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        SwingUtilities.invokeLater(() -> createGui());
    }

    static Image createImage() {
        BufferedImage i = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = (Graphics2D) i.getGraphics();
        g2.setColor(Color.RED);
        g2.fill(new Ellipse2D.Float(0, 0, i.getWidth(), i.getHeight()));
        g2.dispose();
        return i;
    }

    static JPopupMenu createJPopupMenu() {
        final JPopupMenu m = new JPopupMenu();
        m.add(new JMenuItem("Item 1"));
        m.add(new JMenuItem("Item 2"));
        JMenu submenu = new JMenu("Submenu");
        submenu.add(new JMenuItem("item 1"));
        submenu.add(new JMenuItem("item 2"));
        submenu.add(new JMenuItem("item 3"));
        m.add(submenu);
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        m.add(exitItem);
        return m;
    }

    @Override
    public Dimension getSize() {
        Dimension iconSize = new Dimension(super.getSize());
        switch(SystemUtilities.getOsType()) {
            // macOS icons are slightly smaller than the size reported
            case MAC:
                // Handle retina display
                int macScale = MacUtilities.getScaleFactor();

                // Handle undocumented icon border (e.g. 20px has 16px icon)
                // See also IconCache.fixTrayIcons()
                iconSize.width -= iconSize.width / 5;
                iconSize.height -= iconSize.height / 5;

                return new Dimension(iconSize.width * macScale, iconSize.height * macScale);
            case WINDOWS:
                if(Constants.JAVA_VERSION.getMajorVersion() >= 9) {
                    // JDK9+ required for HiDPI tray icons on Windows
                    double winScale = WindowsUtilities.getScaleFactor();

                    // Handle undocumented HiDPI icon support
                    // Requires TrayIcon.setImageAutoSize(true);
                    iconSize.width *= winScale;
                    iconSize.height *= winScale;
                }
                break;
        }
        return iconSize;
    }
}