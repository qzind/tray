/*
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 *
 */

package qz.ui.tray;

import qz.ui.component.IconCache;
import qz.utils.SystemUtilities;
import qz.utils.WindowsUtilities;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Arrays;

/**
 * A TrayIcon-less JPopupMenu container inspired by SwingLabs/JDesktop's <code>JXTrayIcon</code>
 * with special handling for various OSs
 */
public class TrayMenu {
    public interface WantsMenu {
        // implement this interface on a component and we'll listen on it for menu events
    }
    private static final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    private final JFrame frame;
    private final double scaleFactor;

    private int x = 0;
    private int y = 0;

    public TrayMenu() {
        this.scaleFactor = SystemUtilities.isWindows() ? WindowsUtilities.getScaleFactor() : 1.0;
        this.frame = new JFrame();
        this.frame.setAlwaysOnTop(true);
        this.frame.setUndecorated(true);
        this.frame.setBackground(Color.BLACK);
        this.frame.setSize(0, 0);

        // Disable the frame if there's a TrayIcon to attach to
        switch(SystemUtilities.getOs()) {
            case WINDOWS, MAC -> frame.setType(JFrame.Type.UTILITY);
            case LINUX -> frame.setType(JFrame.Type.POPUP);
        }

        frame.pack();
    }

    public void setJPopupMenu(final JPopupMenu popup) {
        Arrays.stream(popup.getPopupMenuListeners()).forEach(popup::removePopupMenuListener);
        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { frame.setVisible(false); }
            @Override public void popupMenuCanceled(PopupMenuEvent e) { frame.setVisible(false); }
        });

        frame.addWindowListener(new WindowListener() {
            @Override
            public void windowActivated(WindowEvent we) {
                popup.setInvoker(frame);
                popup.setVisible(true);
                popup.setLocation(x, y > screenSize.getHeight() / 2 ? y - popup.getHeight() : y);
                popup.requestFocus();
            }

            @Override public void windowOpened(WindowEvent we) {}
            @Override public void windowClosing(WindowEvent we) {}
            @Override public void windowClosed(WindowEvent we) {}
            @Override public void windowIconified(WindowEvent we) {}
            @Override public void windowDeiconified(WindowEvent we) {}
            @Override public void windowDeactivated(WindowEvent we) {}
        });

        addTrayListener();
    }

    public void setIcon(IconCache.Icon icon) {
        if (frame != null) {
            frame.setIconImages(IconCache.getInstance().getImages(icon, SystemUtilities.isDarkTaskbar()));
        }
    }

    /**
     * Functional equivalent of a <code>MouseAdapter</code>, but accommodates an edge-case in Gnome3 where the tray
     * icon cannot listen on mouse events.
     */
    private void addTrayListener() {
        Toolkit.getDefaultToolkit().addAWTEventListener(e -> {
            Point p = isMenuEvent(e);
            if (p != null) {
                x = p.x;
                y = p.y;
                frame.setVisible(true);
                frame.requestFocus();
            }
        }, MouseEvent.MOUSE_EVENT_MASK | AWTEvent.WINDOW_FOCUS_EVENT_MASK);
    }

    private boolean startupCheck = !SystemUtilities.isWindows();

    /**
     * Determines if TrayIcon event is detected
     * @param e An AWTEvent
     * @return A Point on the screen which the tray event occurred, or null if none is found
     */
    private Point isMenuEvent(AWTEvent e) {
        if (e instanceof WindowEvent we) {
            if (we.getSource() instanceof WantsMenu && we.getID() == WindowEvent.WINDOW_GAINED_FOCUS) {
                if(startupCheck) {
                    startupCheck = false;
                    return null;
                }

                if (we.getOppositeWindow() == null) {
                    // FIXME:  This still triggers when we don't wan it to
                    return MouseInfo.getPointerInfo().getLocation();
                }
            }
        }

        if (e instanceof MouseEvent me) {
            if (me.getID() == MouseEvent.MOUSE_RELEASED && me.getSource() != null) {
                if (me.getSource() instanceof TrayIcon) {
                    if(scaleFactor != 1.0) {
                        Point p = me.getLocationOnScreen();
                        // awt is unreliable in high dpi mode
                        return new Point((int)(p.x / scaleFactor), (int)(p.y / scaleFactor));
                    }
                    return me.getLocationOnScreen();
                }
            }
        }
        return null;
    }

    static void main() {
        SystemUtilities.setSystemLookAndFeel();
        SwingUtilities.invokeLater(
                () -> {
                    TrayIcon trayIcon = new TrayIcon(IconCache.getInstance().getImage(IconCache.Icon.TRAY_LOADING_COLOR));
                    TrayMenu tray = new TrayMenu();
                    tray.setJPopupMenu(createJPopupMenu());
                    try {
                        SystemTray.getSystemTray().add(trayIcon);
                    } catch (AWTException e) {
                        System.err.println("Unable to create WindowsTrayIcon: " + e.getMessage());
                    }
                }
        );
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
        exitItem.addActionListener(e -> System.exit(0));
        m.add(exitItem);
        return m;
    }
}