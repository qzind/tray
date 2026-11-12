package qz.ui.tray.fallback;

import qz.common.Constants;
import qz.ui.BasicDialog;
import qz.ui.ThemeUtilities;
import qz.ui.Themeable;
import qz.ui.component.IconCache;
import qz.utils.SystemUtilities;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;

/**
 * Created by Tres on 11/16/2022
 * A container for all the System Tray menu items
 */
public class TaskBarIcon extends BasicDialog implements Themeable {
    private JLabel trayIcon;
    IconCache.Icon icon;

    public TaskBarIcon() {
        super(Constants.ABOUT_TITLE, IconCache.getInstance());
        initComponents();
    }

    public void setJPopupMenu(JPopupMenu popup) {
        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                trayIcon.setOpaque(false);
                trayIcon.repaint();
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {}
        });
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                trayIcon.setOpaque(true);
                trayIcon.repaint();
                if(!popup.isVisible()) {
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private void initComponents() {
        trayIcon = new JLabel(String.format("%s %s", Constants.ABOUT_TITLE, Constants.VERSION));
        trayIcon.setBackground(UIManager.getColor("MenuItem.selectionBackground"));
        trayIcon.setOpaque(false);
        trayIcon.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContent(trayIcon, false);

        // Only allow minimizing, never closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                TaskBarIcon.this.setExtendedState(JFrame.ICONIFIED);
            }
        });

        setResizable(false);
        pack();
        setLocationRelativeTo(null);    // center on main display
    }

    @Override
    public void setVisible(boolean b) {
        // Prevent closing if persistent mode is enabled
        if(!b) {
            setExtendedState(JFrame.ICONIFIED);
        } else {
            super.setVisible(b);
            setExtendedState(JFrame.NORMAL);
        }
    }

    @Override
    public void refresh() {
        if(icon != null) {
            setIcon(icon);
        }
        ThemeUtilities.refreshAll(this);
    }

    public void setIcon(IconCache.Icon icon) {
        this.icon = icon;
        setIconImages(IconCache.getInstance().getImages(icon));
        switch(icon) {
            // FIXME:  Why does macOS always show the white icon?
            case TRAY_LOADING, TRAY_LOADING_COLOR -> {
                trayIcon.setIcon(IconCache.getInstance().getIcon(IconCache.Icon.TRAY_LOADING, SystemUtilities.isDarkDesktop()));
            }
            case TASK_BAR_ICON, TRAY_READY, TRAY_READY_COLOR -> {
                trayIcon.setIcon(IconCache.getInstance().getIcon(IconCache.Icon.TRAY_READY, SystemUtilities.isDarkDesktop()));
            }
            default -> {
                trayIcon.setForeground(Color.RED);
            }
        }
        trayIcon.repaint();
        pack();
    }
}