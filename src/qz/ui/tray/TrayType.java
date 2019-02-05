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
    DIALOG;

    private JXTrayIcon tray = null;
    private DialogTrayIcon dialog = null;

    public JXTrayIcon tray() { return tray; }
    public DialogTrayIcon dialog() { return dialog; }

    public TrayType init() {
        return init(null);
    }

    public TrayType init(ActionListener exitListener) {
        switch (this) {
            case JX:
                tray = new JXTrayIcon(blankImage());
            case CLASSIC:
                tray = new ClassicTrayIcon(blankImage());
            case MODERN:
                tray = new ModernTrayIcon(blankImage());
            default:
                dialog = new DialogTrayIcon(blankImage(), exitListener);
        }
        return this;
    }

    private static Image blankImage() {
        return new ImageIcon(new byte[1]).getImage();
    }

    public boolean isTray() { return tray != null; }

    public boolean isDialog() { return dialog != null; }

    public void setImage(Image image) {
        if (isTray()) {
            tray.setImage(image);
        } else {
            dialog.setImage(image);
        }
    }

    public Dimension getSize() {
        return isTray() ? tray.getSize() : dialog.getSize();
    }

    public void setToolTip(String tooltip) {
        if (isTray()) {
            tray.setToolTip(tooltip);
        } else {
            dialog.setToolTip(tooltip);
        }
    }

    public void setJPopupMenu(JPopupMenu popup) {
        if (isTray()) {
            tray.setJPopupMenu(popup);
        } else {
            dialog.setJPopupMenu(popup);
        }
    }

    public void displayMessage(String caption, String text, TrayIcon.MessageType level) {
        if (isTray()) {
            tray.displayMessage(caption, text, level);
        } else {
            dialog.displayMessage(caption, text, level);
        }
    }

    public void showDialog() {
        if (isDialog()) {
            dialog.setVisible(true);
            dialog.setState(Frame.ICONIFIED);
        }
    }
}
