package qz.ui.tray;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.ui.ThemeUtilities;
import qz.ui.component.IconCache;
import qz.ui.component.iconcache.Saturation;
import qz.ui.tray.fallback.TaskBarIcon;
import qz.ui.tray.mac.MacTrayIcon;
import qz.ui.tray.windows.WindowsTrayIcon;
import qz.utils.SystemUtilities;

import javax.swing.*;
import java.awt.*;

/**
 * Wrapper class to allow popup menu on a tray-less OS
 * @author Tres Finocchiaro
 */
public class TrayWrapper {
    private static final Logger log = LogManager.getLogger(TrayWrapper.class);

    private AbstractSwingTrayIcon tray = null;
    private TaskBarIcon taskbar = null;
    public AbstractSwingTrayIcon tray() { return tray; }

    public TrayWrapper() {
        init(false);
    }

    void init(boolean traySupported) {
        if(traySupported) {
            tray = switch(SystemUtilities.getOs()) {
                case MAC -> new MacTrayIcon();
                case WINDOWS -> new WindowsTrayIcon();
                default -> null;
            };
        }

        taskbar = new TaskBarIcon();
    }

    public void activate() {
        if(tray != null) {
            try {
                SystemTray.getSystemTray().add(tray);
            }
            catch(AWTException awt) {
                log.error("Could not attach tray, falling back to taskbar mode");
                init(true);
            }
        } else {
            showTaskbar();
        }
    }

    public boolean getTaskbar() { return taskbar != null; }

    public void setIcon(IconCache.Icon icon) {
        if (tray != null) {
            tray.setImage(
                    // Use TRAY_LOADING, TRAY_READY if the underlying system prefers it
                    IconCache.getInstance().getImage(
                            icon.getIcon(ThemeUtilities.getTraySaturation()),
                            tray.getSize(),
                            SystemUtilities.isDarkTaskbar(false)));
        } else {
            // Try to swap out the taskbar icon if the tray is missing
            IconCache.Icon fixed = switch(icon) {
                case TRAY_READY, TRAY_READY_COLOR -> IconCache.Icon.TASK_BAR_ICON;
                default -> icon.getIcon(Saturation.COLOR);
            };
            taskbar.setIconImages(IconCache.getInstance().getImages(fixed));
        }
    }

    public void setImage(Image image) {
        if (tray != null) {
            tray.setImage(image);
        } else {
            taskbar.setIconImage(image);
        }
    }

    public Dimension getSize() {
        return tray != null ? tray.getSize() : taskbar.getSize();
    }

    public void setToolTip(String tooltip) {
        if (tray != null) {
            tray.setToolTip(tooltip);
        }
    }

    public void setJPopupMenu(JPopupMenu popup) {
        if (tray != null) {
            tray.setJPopupMenu(popup);
        } else {
            taskbar.setJPopupMenu(popup);
        }
    }

    public void displayMessage(String caption, String text, TrayIcon.MessageType level) {
        if (tray != null) {
            tray.displayMessage(caption, text, level);
        } else {
            // FIXME: taskbar.displayMessage(caption, text, level);
        }
    }

    public void showTaskbar() {
        if (getTaskbar()) {
            taskbar.setVisible(true);
            taskbar.setState(Frame.ICONIFIED);
        }
    }
}
