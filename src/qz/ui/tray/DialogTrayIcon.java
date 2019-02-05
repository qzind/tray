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
    private JPopupMenu popup;

    public DialogTrayIcon(Image trayImage, final ActionListener exitListener) {
        super(Constants.ABOUT_TITLE);
        initializeComponents(trayImage, exitListener);
    }

    private void initializeComponents(Image trayImage, final ActionListener exitListener) {
        // must come first
        setUndecorated(true);
        setTaskBarTitle();

        iconSize = new Dimension(40, 40);
        popupLabel = new JLabel();

        setImage(trayImage);
        setResizable(false);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                exitListener.actionPerformed(new ActionEvent(e.getComponent(), e.getID(), "Exit"));
            }
        });

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        if (SystemUtilities.isUbuntu()) {
            panel.setBackground(UbuntuUtilities.getTrayColor());
        }
        popupLabel.addMouseListener(mouseListener);
        popupLabel.addMouseMotionListener(mouseMotionListener);
        panel.add(popupLabel);
        setContentPane(panel);
        pack();
    }

    // makes drag-able by icon
    private MouseMotionListener mouseMotionListener = new MouseMotionListener() {
        @Override
        public void mouseDragged(MouseEvent e) {
            Point p = getLocation();
            setLocation(p.x + (e.getX() - dragPoint.x), p.y + (e.getY() - dragPoint.y));
        }

        @Override
        public void mouseMoved(MouseEvent e) {}
    };

    // shows menu when icon is clicked
    private MouseListener mouseListener = new MouseListener() {
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
    };

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

    public void setImage(Image trayImage) {
        popupLabel.setIcon(new ImageIcon(trayImage));
        setIconImage(trayImage);
        pack();
    }

    public void setToolTip(String tooltip) {
        popupLabel.setToolTipText(tooltip);
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

        // add hide menu items
        final JMenuItem hide = new JMenuItem("Hide");
        hide.setMnemonic(KeyEvent.VK_H);
        hide.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                setState(JFrame.ICONIFIED);
            }
        });
        this.popup.add(hide, 0);
        this.popup.add(new JSeparator());
    }

    public void displayMessage(String caption, String text, TrayIcon.MessageType level) { /* noop */ }
}
