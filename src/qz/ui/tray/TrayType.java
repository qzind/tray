package qz.ui.tray;

import org.jdesktop.swinghelper.tray.JXTrayIcon;

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

    public JXTrayIcon tray() { return tray; }

    public TrayType init() {
        return init(null);
    }

    public TrayType init(ActionListener exitListener) {
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
        return this;
    }

    private static Image blankImage() {
        return new ImageIcon(new byte[1]).getImage();
    }

    public boolean isTray() { return tray != null; }

    public boolean getTaskbar() { return taskbar != null; }

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
