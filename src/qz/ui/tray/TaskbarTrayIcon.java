package qz.ui.tray;

import qz.common.Constants;
import qz.utils.SystemUtilities;
import qz.utils.UbuntuUtilities;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Field;

public class TaskbarTrayIcon extends JFrame implements WindowListener {
    private Dimension iconSize;
    private JPopupMenu popup;

    public TaskbarTrayIcon(Image trayImage, final ActionListener exitListener) {
        super(Constants.ABOUT_TITLE);
        initializeComponents(trayImage, exitListener);
    }

    private void initializeComponents(Image trayImage, final ActionListener exitListener) {
        // must come first
        setUndecorated(true);
        setTaskBarTitle();
        setSize(0,0);
        getContentPane().setBackground(Color.BLACK);
        if (SystemUtilities.isUbuntu()) {
            // attempt to camouflage the single pixel left behind
            getContentPane().setBackground(UbuntuUtilities.getTrayColor());
        }

        iconSize = new Dimension(40, 40);

        setIconImage(trayImage);
        setResizable(false);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                exitListener.actionPerformed(new ActionEvent(e.getComponent(), e.getID(), "Exit"));
            }
        });
        addWindowListener(this);
    }

    // fixes Linux taskbar title per http://hg.netbeans.org/core-main/rev/5832261b8434
    public void setTaskBarTitle() {
        try {
            Class<?> toolkit = Toolkit.getDefaultToolkit().getClass();
            if (toolkit.getName().equals("sun.awt.X11.XToolkit")) {
                try {
                    final Field awtAppClassName = toolkit.getDeclaredField("awtAppClassName");
                    awtAppClassName.setAccessible(true);
                    awtAppClassName.set(null, getTitle());
                }
                catch(Exception e) {}
            }
        } catch(Exception ignore) {}
    }

    /**
     * Returns the "tray" icon size (not the dialog size)
     */
    @Override
    public Dimension getSize() {
        return iconSize;
    }

    public void setJPopupMenu(final JPopupMenu popup) {
        this.popup = popup;
        this.popup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {}

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                setState(JFrame.ICONIFIED);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                setState(JFrame.ICONIFIED);
            }
        });
    }

    public void displayMessage(String caption, String text, TrayIcon.MessageType level) { /* noop */ }

    @Override
    public void windowDeiconified(WindowEvent e) {
        Point p = MouseInfo.getPointerInfo().getLocation();
        setLocation(p);
        popup.show(this, 0,0);
        popup.setLocation(p);
    }

    @Override
    public void windowOpened(WindowEvent windowEvent) {}

    @Override
    public void windowClosing(WindowEvent windowEvent) {}

    @Override
    public void windowClosed(WindowEvent windowEvent) {}

    @Override
    public void windowIconified(WindowEvent windowEvent) {}

    @Override
    public void windowActivated(WindowEvent windowEvent) {}

    @Override
    public void windowDeactivated(WindowEvent windowEvent) {
        setState(JFrame.ICONIFIED);
    }
}
