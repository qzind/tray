package qz.ui.tray.windows;

import qz.ui.component.IconCache;
import qz.ui.tray.AbstractSwingTrayIcon;
import qz.ui.tray.TrayMenu;
import qz.utils.SystemUtilities;
import qz.utils.WindowsUtilities;

import javax.swing.*;
import java.awt.*;

public class WindowsTrayIcon extends AbstractSwingTrayIcon implements TrayMenu.WantsMenu {
    private final Dimension size;

    public WindowsTrayIcon() {
        super();
        setImageAutoSize(true); // undocumented high dpi behavior
        size = calculateSize();
    }

    @Override
    public void setJPopupMenu(JPopupMenu menu) {
        new TrayMenu().setJPopupMenu(menu);
    }

    @Override
    public void setIcon(IconCache.Icon icon) {
        setImage(IconCache.getInstance().getImage(icon, size, SystemUtilities.isDarkTaskbar()));
    }

    /**
     * Windows misreports the tray size on high dpi displays
     */
    public Dimension calculateSize() {
        double scaleFactor = WindowsUtilities.getScaleFactor();
        if(scaleFactor > 1.0) {
            int w = super.getSize().width;
            int h = super.getSize().height;
            return new Dimension((int)(w * scaleFactor), (int)(h * scaleFactor));
        }
        return super.getSize();
    }


    @Override
    public Dimension getSize() {
        return size;
    }
}
