package qz.ui;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.SystemUtilities;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ThemeUtilities {

    /**
     * Polling thread to check if a meaningful Desktop theme has changed
     */
    public static class ThemeMonitor {
        private static final Logger log = LogManager.getLogger(ThemeMonitor.class);
        private static final String LOG_TEMPLATE = "%s theme changed from '%s' to '%s'";
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        private Runnable refreshAction;

        private boolean isDarkDesktop;
        private boolean isDarkTaskbar;

        public ThemeMonitor() {
            this.isDarkDesktop = SystemUtilities.isDarkDesktop(false);
            this.isDarkTaskbar = SystemUtilities.isDarkTaskbar(false);
        }

        public void onChange(Runnable refreshAction) {
            this.refreshAction = refreshAction;
        }

        public ThemeMonitor startPolling(long intervalMs) {
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    boolean isDarkDesktop = SystemUtilities.isDarkDesktop(true);
                    boolean isDarkTaskbar = SystemUtilities.isDarkTaskbar(true);

                    if(this.isDarkDesktop != isDarkDesktop || this.isDarkTaskbar != isDarkTaskbar) {
                        String desktopMessage = format("Desktop", this.isDarkDesktop, isDarkDesktop);
                        String taskbarMessage = format("Taskbar", this.isDarkTaskbar, isDarkTaskbar);
                        if(!desktopMessage.isEmpty()) {
                            log.info(desktopMessage);
                        }
                        if(!taskbarMessage.isEmpty()) {
                            log.info(taskbarMessage);
                        }

                        this.isDarkDesktop = isDarkDesktop;
                        this.isDarkTaskbar = isDarkTaskbar;
                        if(refreshAction != null) {
                            this.refreshAction.run();
                        }
                    }
                } catch (Throwable t) {
                    log.warn("An error occurred polling the light/dark theme: {}", t.getMessage());
                }
            }, 0, intervalMs, TimeUnit.MILLISECONDS);
            return this;
        }

        public static String format(String desktopComponent, boolean wasDark, boolean isDark) {
            if(wasDark == isDark) {
                return "";
            }
            return String.format(LOG_TEMPLATE, desktopComponent, wasDark? "dark":"light", isDark? "dark":"light");
        }
    }

    public static void refreshAll(Container container, Component ... orphans) {
        // Handle orphaned UI objects (e.g. Component added to a message dialog)
        for(Component orphan : orphans) {
            recurseOrphanedComponents(orphan);
        }
        refreshAll(ArrayUtils.addAll(container.getComponents(), orphans));
    }

    private static void refreshAll(Component ... components) {
        for(Component c : components) {
            if (c instanceof Themeable) {
                ((Themeable)c).refresh();
            }
            if (c instanceof Container) {
                refreshAll((Container)c);
            }
        }
    }

    /**
     * Inefficient yet effective way to recurse orphaned component's UI changes
     */
    private static Container recurseOrphanedComponents(Component c) {
        if (c != null) {
            SwingUtilities.updateComponentTreeUI(c);
            if (c instanceof JRootPane) {
                return (Container)c;
            }
            return recurseOrphanedComponents(c.getParent());
        }
        return null;
    }
}
