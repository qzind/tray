package qz.ui.tray;

import org.jdesktop.swinghelper.tray.JXTrayIcon;
import qz.ui.component.IconCache;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Wrapper class to allow popup menu on a tray-less OS
 * @author Tres Finocchiaro
 */
public enum TrayType {
    JX,
    CLASSIC,
    MODERN,
    TASKBAR;

    private JXTrayIcon tray = null;
    private TaskbarTrayIcon taskbar = null;
    private IconCache iconCache;

    public JXTrayIcon tray() { return tray; }

    public TrayType init(IconCache iconCache) {
        return init(null, iconCache);
    }

    public TrayType init(ActionListener exitListener, IconCache iconCache) {
        switch (this) {
            case JX:
                tray = new JXTrayIcon(blankImage()); break;
            case CLASSIC:
                tray = new ClassicTrayIcon(blankImage()); break;
            case MODERN:
                tray = new ModernTrayIcon(blankImage()); break;
            default:
                taskbar = new TaskbarTrayIcon(blankImage(), exitListener);
        }
        this.iconCache = iconCache;
        return this;
    }

    private static Image blankImage() {
        return new ImageIcon(new byte[1]).getImage();
    }

    public boolean isTray() { return tray != null; }

    public boolean getTaskbar() { return taskbar != null; }

    public void setIcon(IconCache.Icon icon) {
        if (isTray()) {
            tray.setImage(iconCache.getImage(icon, tray.getSize()));
        } else {
            taskbar.setIconImages(iconCache.getImages(icon));
        }
    }

    public void setImage(Image image) {
        if (isTray()) {
            tray.setImage(image);
        } else {
            taskbar.setIconImage(image);
        }
    }

    public void setImageAutoSize(boolean autoSize) {
        if (isTray()) {
            tray.setImageAutoSize(autoSize);
        }
    }

    public Dimension getSize() {
        return isTray() ? tray.getSize() : taskbar.getSize();
    }

    public void setToolTip(String tooltip) {
        if (isTray()) {
            tray.setToolTip(tooltip);
        }
    }

    public void setJPopupMenu(JPopupMenu popup) {
        if (isTray()) {
            tray.setJPopupMenu(popup);
        } else {
            taskbar.setJPopupMenu(popup);
        }
    }

    public void displayMessage(String caption, String text, TrayIcon.MessageType level) {
        if (isTray()) {
            tray.displayMessage(caption, text, level);
        } else {
            taskbar.displayMessage(caption, text, level);
        }
    }

    public void showTaskbar() {
        if (getTaskbar()) {
            taskbar.setVisible(true);
            taskbar.setState(Frame.ICONIFIED);
        }
    }
}
