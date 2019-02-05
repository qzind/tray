package qz.ui.tray;

import qz.common.Constants;
import qz.utils.SystemUtilities;
import qz.utils.UbuntuUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Field;

public class DialogTrayIcon extends JFrame {
    private Dimension iconSize;
    private JLabel popupLabel;
    private Point dragPoint;

    public DialogTrayIcon(Image trayImage, final ActionListener exitListener) {
        super(Constants.ABOUT_TITLE);
        iconSize = new Dimension(40, 40);
        popupLabel = new JLabel();
        setImage(trayImage);
        setUndecorated(true);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                exitListener.actionPerformed(new ActionEvent(e.getComponent(), e.getID(), "Exit"));
            }
        });
        setTitle();
    }

    /**
     * Fix Linux taskbar title per http://hg.netbeans.org/core-main/rev/5832261b8434
     */
    public void setTitle() {
        Class<?> toolkit = Toolkit.getDefaultToolkit().getClass();
        if (toolkit.getName().equals("sun.awt.X11.XToolkit")) {
            try {
                final Field awtAppClassName = toolkit.getDeclaredField("awtAppClassName");
                awtAppClassName.setAccessible(true);
                awtAppClassName.set(null, getTitle());
            } catch (Exception e) {}
        }
    }

    public void setImage(Image trayImage) {
        popupLabel.setIcon(new ImageIcon(trayImage));
        setIconImage(trayImage);
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
        // Add an option to hide it
        final JMenuItem hideMenuItem = new JMenuItem("Hide");
        hideMenuItem.setMnemonic(KeyEvent.VK_H);
        hideMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                setState(JFrame.ICONIFIED);
            }
        });
        popup.add(hideMenuItem, 0);
        popup.add(new JSeparator());

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        if (SystemUtilities.isUbuntu()) {
            panel.setBackground(UbuntuUtilities.getTrayColor());
        }
        popupLabel.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent)  {
                popup.show(popupLabel, mouseEvent.getX(), mouseEvent.getY());
            }
            @Override
            public void mousePressed(MouseEvent e) {
                dragPoint = e.getPoint();
            }
            @Override
            public void mouseReleased(MouseEvent e) {}
            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mouseExited(MouseEvent e) {}

        });
        popupLabel.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point p = getLocation();
                setLocation(p.x + (e.getX() - dragPoint.x), p.y + (e.getY() - dragPoint.y));
            }

            @Override
            public void mouseMoved(MouseEvent e) {}
        });
        panel.add(popupLabel);
        setContentPane(panel);
        pack();
    }

    public void displayMessage(String caption, String text, TrayIcon.MessageType level) { /* noop */ }
}
