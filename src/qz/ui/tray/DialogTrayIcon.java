package qz.ui.tray;

import qz.common.Constants;
import qz.utils.SystemUtilities;
import qz.utils.UbuntuUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class DialogTrayIcon extends JFrame {
    private Dimension iconSize;
    private JLabel popupLabel;

    public DialogTrayIcon(Image trayImage) {
        super(Constants.ABOUT_TITLE);
        iconSize = new Dimension(40, 40);
        popupLabel = new JLabel();
        setImage(trayImage);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                setExtendedState(JFrame.ICONIFIED);
            }
        });
    }

    public void setImage(Image trayImage) {
        popupLabel.setIcon(new ImageIcon(trayImage));
    }

    public void setToolTip(String tooltip) {
        popupLabel.setToolTipText(tooltip);
    }

    /**
     * Return the "tray" icon size
     */
    @Override
    public Dimension getSize() {
        return iconSize;
    }

    public void setJPopupMenu(final JPopupMenu popup) {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        if (SystemUtilities.isUbuntu()) {
            panel.setBackground(UbuntuUtilities.getTrayColor());
        }
        popupLabel.addMouseListener(new MouseListener() {
            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                popup.show(popupLabel, mouseEvent.getX(), mouseEvent.getY());
            }
            @Override
            public void mouseReleased(MouseEvent mouseEvent) {}
            @Override
            public void mouseEntered(MouseEvent mouseEvent) {}
            @Override
            public void mouseExited(MouseEvent mouseEvent) {}
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {}

        });
        panel.add(popupLabel);
        setContentPane(panel);
        pack();
    }

    public void displayMessage(String caption, String text, TrayIcon.MessageType level) { /* noop */ }
}
