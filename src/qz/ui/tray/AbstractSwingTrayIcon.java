package qz.ui.tray;

import qz.ui.component.IconCache;

import javax.swing.*;
import java.awt.*;

/**
 * Interface for JPopupMenu-wrapped TrayIcon implementations
 */
public abstract class AbstractSwingTrayIcon extends TrayIcon {
    public AbstractSwingTrayIcon() {
        super(new ImageIcon(new byte[1]).getImage());
    }

    public abstract void setJPopupMenu(JPopupMenu menu);

    public abstract void setIcon(IconCache.Icon icon);
}
