package qz.ui.tray.fallback;

import qz.common.Constants;
import qz.ui.component.IconCache;
import qz.ui.tray.TrayMenu;
import qz.utils.SystemUtilities;

import javax.swing.*;
import java.awt.*;

/**
 * A wrapper around System Tray functionality for when no system tray is available
 */
public class TaskBarIcon extends JFrame implements TrayMenu.WantsMenu {
    public TaskBarIcon() {
        super(Constants.ABOUT_TITLE);
        setUndecorated(true);
        setSize(0, 0);
        getContentPane().setBackground(Color.BLACK);
        setResizable(false);
        setState(JFrame.ICONIFIED);
    }

    public void setJPopupMenu(JPopupMenu popup) {
        new TrayMenu().setJPopupMenu(popup);
    }

    static void main() {
        SystemUtilities.setSystemLookAndFeel();
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

        TaskBarIcon frame = new TaskBarIcon();
        frame.setIconImages(IconCache.getInstance().getImages(IconCache.Icon.TRAY_READY_COLOR));
        frame.setVisible(true);
        frame.setJPopupMenu(m);
    }
}

