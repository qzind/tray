/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 *
 */

package qz.ui.tray;

import org.jdesktop.swinghelper.tray.JXTrayIcon;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/**
 * @author A. Tres Finocchiaro
 */
public class ModernTrayIcon extends JXTrayIcon {
    private static Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    private JFrame invisibleFrame;
    private JPopupMenu popup;
    private int x = 0, y = 0;

    public ModernTrayIcon(Image image) {
        super(image);
    }

    @Override
    public void setJPopupMenu(final JPopupMenu popup) {
        this.popup = popup;

        invisibleFrame = new JFrame();
        invisibleFrame.setAlwaysOnTop(true);
        invisibleFrame.setUndecorated(true);
        invisibleFrame.setBackground(Color.BLACK);
        invisibleFrame.setSize(0, 0);
        invisibleFrame.pack();

        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { invisibleFrame.setVisible(false); }
            @Override public void popupMenuCanceled(PopupMenuEvent e) { invisibleFrame.setVisible(false); }
        });

        invisibleFrame.addWindowListener(new WindowListener() {
            @Override
            public void windowActivated(WindowEvent we) {
                popup.setInvoker(invisibleFrame);
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

    @Override
    public void setImage(Image image) {
        super.setImage(image);
        if (invisibleFrame != null) {
            invisibleFrame.setIconImage(image);
        }
    }

    public JPopupMenu getJPopupMenu() {
        return popup;
    }

    /**
     * Functional equivalent of a <code>MouseAdapter</code>, but accommodates an edge-case in Gnome3 where the tray
     * icon cannot listen on mouse events.
     */
    private void addTrayListener() {
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent e) {
                Point p = isTrayEvent(e);
                if (p != null) {
                    x = p.x; y = p.y;
                    invisibleFrame.setVisible(true);
                    invisibleFrame.requestFocus();
                }
            }
        }, MouseEvent.MOUSE_EVENT_MASK);
    }

    /**
     * Determines if TrayIcon event is detected
     * @param e An AWTEvent
     * @return A Point on the screen which the tray event occurred, or null if none is found
     */
    private static Point isTrayEvent(AWTEvent e) {
        if (e instanceof MouseEvent) {
            MouseEvent me = (MouseEvent)e;

            if (me.getID() == MouseEvent.MOUSE_RELEASED && me.getSource() != null) {
                if (me.getSource().getClass().getName().contains("TrayIcon")) {
                    return me.getLocationOnScreen();
                }
            }
        }
        return null;
    }
}