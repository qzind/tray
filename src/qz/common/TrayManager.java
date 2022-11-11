/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.common;

import com.github.zafarkhaja.semver.Version;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;
import qz.App;
import qz.auth.Certificate;
import qz.auth.RequestState;
import qz.installer.shortcut.ShortcutCreator;
import qz.printer.PrintServiceMatcher;
import qz.printer.action.html.WebApp;
import qz.ui.*;
import qz.ui.component.IconCache;
import qz.ui.tray.TrayType;
import qz.utils.*;
import qz.ws.PrintSocketServer;
import qz.ws.SingleInstanceChecker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static qz.ui.component.IconCache.Icon.*;

/**
 * Manages the icons and actions associated with the TrayIcon
 *
 * @author Tres Finocchiaro
 */
public class TrayManager {

    private static final Logger log = LogManager.getLogger(TrayManager.class);

    private boolean headless;

    // The cached icons
    private final IconCache iconCache;

    // Custom swing pop-up menu
    private TrayType tray;

    private ConfirmDialog confirmDialog;
    private GatewayDialog gatewayDialog;
    private AboutDialog aboutDialog;
    private LogDialog logDialog;
    private SiteManagerDialog sitesDialog;
    private ArrayList<Component> componentList;
    private IconCache.Icon shownIcon;

    // Need a class reference to this so we can set it from the request dialog window
    private JCheckBoxMenuItem anonymousItem;

    // The name this UI component will use, i.e "QZ Print 1.9.0"
    private final String name;

    // The shortcut and startup helper
    private final ShortcutCreator shortcutCreator;

    private final PropertyHelper prefs;

    // Action to run when reload is triggered
    private Thread reloadThread;

    // Actions to run if idle after startup
    private java.util.Timer idleTimer = new java.util.Timer();

    public TrayManager() {
        this(false);
    }

    /**
     * Create a AutoHideJSystemTray with the specified name/text
     */
    public TrayManager(boolean isHeadless) {
        name = Constants.ABOUT_TITLE + " " + Constants.VERSION;

        prefs = new PropertyHelper(FileUtilities.USER_DIR + File.separator + Constants.PREFS_FILE + ".properties");

        // Set strict certificate mode preference
        Certificate.setTrustBuiltIn(!getPref(Constants.PREFS_STRICT_MODE, false));

        // Set FileIO security
        FileUtilities.setFileIoEnabled(getPref(Constants.PREFS_FILEIO_ENABLED, true));
        FileUtilities.setFileIoStrict(getPref(Constants.PREFS_FILEIO_STRICT, false));

        // Headless if turned on by user or unsupported by environment
        headless = isHeadless || getPref(Constants.PREFS_HEADLESS, false) || GraphicsEnvironment.isHeadless();
        if (headless) {
            log.info("Running in headless mode");
        }

        // Set up the shortcut name so that the UI components can use it
        shortcutCreator = ShortcutCreator.getInstance();

        SystemUtilities.setSystemLookAndFeel();
        iconCache = new IconCache();

        if (SystemUtilities.isSystemTraySupported(headless)) { // UI mode with tray
            switch(SystemUtilities.getOsType()) {
                case WINDOWS:
                    tray = TrayType.JX.init(iconCache);
                    // Undocumented HiDPI behavior
                    tray.setImageAutoSize(true);
                    break;
                case MAC:
                    tray = TrayType.CLASSIC.init(iconCache);
                    break;
                default:
                    tray = TrayType.MODERN.init(iconCache);
            }

            // OS-specific tray icon handling
            if (SystemTray.isSupported()) {
                iconCache.fixTrayIcons(SystemUtilities.isDarkTaskbar());
            }

            // Iterates over all images denoted by IconCache.getTypes() and caches them
            tray.setIcon(DANGER_ICON);
            tray.setToolTip(name);

            try {
                SystemTray.getSystemTray().add(tray.tray());
            }
            catch(AWTException awt) {
                log.error("Could not attach tray, forcing headless mode", awt);
                headless = true;
            }
        } else if (!headless) { // UI mode without tray
            tray = TrayType.TASKBAR.init(exitListener, iconCache);
            tray.setIcon(DANGER_ICON);
            tray.setToolTip(name);
            tray.showTaskbar();
        }

        // TODO: Remove when fixed upstream.  See issue #393
        if (SystemUtilities.isUnix() && !isHeadless) {
            // Update printer list in CUPS immediately (normally 2min)
            System.setProperty("sun.java2d.print.polling", "false");
        }

        if (!headless) {
            componentList = new ArrayList<>();

            // The allow/block dialog
            gatewayDialog = new GatewayDialog(null, "Action Required", iconCache);
            componentList.add(gatewayDialog);

            // The ok/cancel dialog
            confirmDialog = new ConfirmDialog(null, "Please Confirm", iconCache);
            componentList.add(confirmDialog);

            // Detect theme changes
            new Thread(() -> {
                boolean darkDesktopMode = SystemUtilities.isDarkDesktop();
                boolean darkTaskbarMode = SystemUtilities.isDarkTaskbar();
                while(true) {
                    try {
                        Thread.sleep(1000);
                        if (darkDesktopMode != SystemUtilities.isDarkDesktop(true) ||
                                darkTaskbarMode != SystemUtilities.isDarkTaskbar(true)) {
                            darkDesktopMode = SystemUtilities.isDarkDesktop();
                            darkTaskbarMode = SystemUtilities.isDarkTaskbar();
                            iconCache.fixTrayIcons(darkTaskbarMode);
                            refreshIcon(null);
                            SwingUtilities.invokeLater(() -> {
                                SystemUtilities.setSystemLookAndFeel();
                                for(Component c : componentList) {
                                    SwingUtilities.updateComponentTreeUI(c);
                                    if (c instanceof Themeable) {
                                        ((Themeable)c).refresh();
                                    }
                                    if (c instanceof JDialog) {
                                        ((JDialog)c).pack();
                                    } else if (c instanceof JPopupMenu) {
                                        ((JPopupMenu)c).pack();
                                    }
                                }
                            });
                        }
                    }
                    catch(InterruptedException ignore) {}
                }
            }).start();
        }

        if (tray != null) {
            addMenuItems();
        }

        // Initialize idle actions
        // Slow to start JavaFX the first time
        if (getPref(Constants.PREFS_IDLE_JFX, true)) {
            performIfIdle((int)TimeUnit.SECONDS.toMillis(60), evt -> {
                log.debug("IDLE: Starting up JFX for HTML printing");
                try {
                    WebApp.initialize();
                }
                catch(IOException e) {
                    log.error("Idle runner failed to preemptively start JavaFX service");
                }
            });
        }
        // Slow to find printers the first time if a lot of printers are installed
        // Must run after JavaFX per https://github.com/qzind/tray/issues/924
        if (getPref(Constants.PREFS_IDLE_PRINTERS, true)) {
            performIfIdle((int)TimeUnit.SECONDS.toMillis(120), evt -> {
                log.debug("IDLE: Performing first run of find printers");
                PrintServiceMatcher.getNativePrinterList(false, true);
            });
        }
    }

    /**
     * Stand-alone invocation of TrayManager
     *
     * @param args arguments to pass to main
     */
    public static void main(String args[]) {
        SwingUtilities.invokeLater(TrayManager::new);
    }

    /**
     * Builds the swing pop-up menu with the specified items
     */
    private void addMenuItems() {
        JPopupMenu popup = new JPopupMenu();
        componentList.add(popup);

        JMenu advancedMenu = new JMenu("Advanced");
        advancedMenu.setMnemonic(KeyEvent.VK_A);
        advancedMenu.setIcon(iconCache.getIcon(SETTINGS_ICON));

        JMenuItem sitesItem = new JMenuItem("Site Manager...", iconCache.getIcon(SAVED_ICON));
        sitesItem.setMnemonic(KeyEvent.VK_M);
        sitesItem.addActionListener(savedListener);
        sitesDialog = new SiteManagerDialog(sitesItem, iconCache, prefs);
        componentList.add(sitesDialog);

        JMenuItem diagnosticMenu = new JMenu("Diagnostic");

        JMenuItem browseApp = new JMenuItem("Browse App folder...", iconCache.getIcon(FOLDER_ICON));
        browseApp.setToolTipText(SystemUtilities.getJarParentPath().toString());
        browseApp.setMnemonic(KeyEvent.VK_O);
        browseApp.addActionListener(e -> ShellUtilities.browseAppDirectory());
        diagnosticMenu.add(browseApp);

        JMenuItem browseUser = new JMenuItem("Browse User folder...", iconCache.getIcon(FOLDER_ICON));
        browseUser.setToolTipText(FileUtilities.USER_DIR.toString());
        browseUser.setMnemonic(KeyEvent.VK_U);
        browseUser.addActionListener(e -> ShellUtilities.browseDirectory(FileUtilities.USER_DIR));
        diagnosticMenu.add(browseUser);

        JMenuItem browseShared = new JMenuItem("Browse Shared folder...", iconCache.getIcon(FOLDER_ICON));
        browseShared.setToolTipText(FileUtilities.SHARED_DIR.toString());
        browseShared.setMnemonic(KeyEvent.VK_S);
        browseShared.addActionListener(e -> ShellUtilities.browseDirectory(FileUtilities.SHARED_DIR));
        diagnosticMenu.add(browseShared);

        diagnosticMenu.add(new JSeparator());

        JCheckBoxMenuItem notificationsItem = new JCheckBoxMenuItem("Show all notifications");
        notificationsItem.setToolTipText("Shows all connect/disconnect messages, useful for debugging purposes");
        notificationsItem.setMnemonic(KeyEvent.VK_S);
        notificationsItem.setState(getPref(Constants.PREFS_NOTIFICATIONS, false));
        notificationsItem.addActionListener(notificationsListener);
        diagnosticMenu.add(notificationsItem);

        JCheckBoxMenuItem monocleItem = new JCheckBoxMenuItem("Use Monocle for HTML");
        monocleItem.setToolTipText("Use monocle platform for HTML printing (restart required)");
        monocleItem.setMnemonic(KeyEvent.VK_U);
        monocleItem.setState(getPref(Constants.PREFS_MONOCLE, true));
        if(!SystemUtilities.hasMonocle()) {
            log.warn("Monocle engine was not detected");
            monocleItem.setEnabled(false);
            monocleItem.setToolTipText("Monocle HTML engine was not detected");
        }
        monocleItem.addActionListener(monocleListener);

        if (Constants.JAVA_VERSION.greaterThanOrEqualTo(Version.valueOf("11.0.0"))) { //only include if it can be used
            diagnosticMenu.add(monocleItem);
        }

        diagnosticMenu.add(new JSeparator());

        JMenuItem logItem = new JMenuItem("View logs (live feed)...", iconCache.getIcon(LOG_ICON));
        logItem.setMnemonic(KeyEvent.VK_L);
        logItem.addActionListener(logListener);
        diagnosticMenu.add(logItem);
        logDialog = new LogDialog(logItem, iconCache);
        componentList.add(logDialog);

        JMenuItem zipLogs = new JMenuItem("Zip logs (to Desktop)");
        zipLogs.setToolTipText("Zip diagnostic logs, place on Desktop");
        zipLogs.setMnemonic(KeyEvent.VK_Z);
        zipLogs.addActionListener(e -> FileUtilities.zipLogs());
        diagnosticMenu.add(zipLogs);

        JMenuItem desktopItem = new JMenuItem("Create Desktop shortcut", iconCache.getIcon(DESKTOP_ICON));
        desktopItem.setMnemonic(KeyEvent.VK_D);
        desktopItem.addActionListener(desktopListener());

        anonymousItem = new JCheckBoxMenuItem("Block anonymous requests");
        anonymousItem.setToolTipText("Blocks all requests that do not contain a valid certificate/signature");
        anonymousItem.setMnemonic(KeyEvent.VK_K);
        anonymousItem.setState(Certificate.UNKNOWN.isBlocked());
        anonymousItem.addActionListener(anonymousListener);

        if(Constants.ENABLE_DIAGNOSTICS) {
            advancedMenu.add(diagnosticMenu);
            advancedMenu.add(new JSeparator());
        }
        advancedMenu.add(sitesItem);
        advancedMenu.add(desktopItem);
        advancedMenu.add(new JSeparator());
        advancedMenu.add(anonymousItem);

        JMenuItem reloadItem = new JMenuItem("Reload", iconCache.getIcon(RELOAD_ICON));
        reloadItem.setMnemonic(KeyEvent.VK_R);
        reloadItem.addActionListener(reloadListener);

        JMenuItem aboutItem = new JMenuItem("About...", iconCache.getIcon(ABOUT_ICON));
        aboutItem.setMnemonic(KeyEvent.VK_B);
        aboutItem.addActionListener(aboutListener);
        aboutDialog = new AboutDialog(aboutItem, iconCache);
        componentList.add(aboutDialog);

        if (SystemUtilities.isMac()) {
            MacUtilities.registerAboutDialog(aboutDialog);
            MacUtilities.registerQuitHandler(this);
        }

        JSeparator separator = new JSeparator();

        JCheckBoxMenuItem startupItem = new JCheckBoxMenuItem("Automatically start");
        startupItem.setMnemonic(KeyEvent.VK_S);
        startupItem.setState(FileUtilities.isAutostart());
        startupItem.addActionListener(startupListener());
        if (!shortcutCreator.canAutoStart()) {
            startupItem.setEnabled(false);
            startupItem.setState(false);
            startupItem.setToolTipText("Autostart has been disabled by the administrator");
        }

        JMenuItem exitItem = new JMenuItem("Exit", iconCache.getIcon(EXIT_ICON));
        exitItem.addActionListener(exitListener);

        popup.add(advancedMenu);
        popup.add(reloadItem);
        popup.add(aboutItem);
        popup.add(startupItem);
        popup.add(separator);
        popup.add(exitItem);

        if (tray != null) {
            tray.setJPopupMenu(popup);
        }
    }


    private final ActionListener notificationsListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            prefs.setProperty(Constants.PREFS_NOTIFICATIONS, ((JCheckBoxMenuItem)e.getSource()).getState());
        }
    };

    private final ActionListener monocleListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            JCheckBoxMenuItem j = (JCheckBoxMenuItem)e.getSource();
            prefs.setProperty(Constants.PREFS_MONOCLE, j.getState());
            displayWarningMessage(String.format("A restart of %s is required to ensure this feature is %sabled.",
                                                Constants.ABOUT_TITLE, j.getState()? "en":"dis"));
        }
    };

    private final ActionListener desktopListener() {
        return e -> {
            shortcutCreator.createDesktopShortcut();
        };
    }

    private final ActionListener savedListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            sitesDialog.setVisible(true);
        }
    };

    private final ActionListener anonymousListener = e -> {
        boolean checkBoxState = true;
        if (e.getSource() instanceof JCheckBoxMenuItem) {
            checkBoxState = ((JCheckBoxMenuItem)e.getSource()).getState();
        }

        log.debug("Block unsigned: {}", checkBoxState);

        if (checkBoxState) {
            blackList(Certificate.UNKNOWN);
        } else {
            FileUtilities.deleteFromFile(Constants.BLOCK_FILE, Certificate.UNKNOWN.data(), true);
            FileUtilities.deleteFromFile(Constants.BLOCK_FILE, Certificate.UNKNOWN.data(), false);
        }
    };

    private final ActionListener logListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            logDialog.setVisible(true);
        }
    };

    private ActionListener startupListener() {
        return e -> {
            JCheckBoxMenuItem source = (JCheckBoxMenuItem)e.getSource();
            if (!source.getState() && !confirmDialog.prompt("Remove " + name + " from startup?")) {
                source.setState(true);
                return;
            }
            if (FileUtilities.setAutostart(source.getState())) {
                displayInfoMessage("Successfully " + (source.getState() ? "enabled" : "disabled") + " autostart");
            } else {
                displayErrorMessage("Error " + (source.getState() ? "enabling" : "disabling") + " autostart");
            }
            source.setState(FileUtilities.isAutostart());
        };
    }

    /**
     * Sets the default reload action (in this case, <code>Thread.start()</code>) to be fired
     *
     * @param reloadThread The Thread to call when reload is clicked
     */
    public void setReloadThread(Thread reloadThread) {
        this.reloadThread = reloadThread;
    }

    private ActionListener reloadListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            if (reloadThread == null) {
                showErrorDialog("Sorry, Reload has not yet been implemented.");
            } else {
                reloadThread.start();
            }
        }
    };

    private final ActionListener aboutListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            aboutDialog.setVisible(true);
        }
    };

    private final ActionListener exitListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            boolean showAllNotifications = getPref(Constants.PREFS_NOTIFICATIONS, false);
            if (!showAllNotifications || confirmDialog.prompt("Exit " + name + "?")) { exit(0); }
        }
    };

    public void exit(int returnCode) {
        prefs.save();
        System.exit(returnCode);
    }

    /**
     * Displays a basic error dialog.
     */
    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(null, message, name, JOptionPane.ERROR_MESSAGE);
    }

    public boolean showGatewayDialog(final RequestState request, final String prompt, final Point position) {
        if (!headless) {
            try {
                SwingUtilities.invokeAndWait(() -> gatewayDialog.prompt("%s wants to " + prompt, request, position));
            }
            catch(Exception ignore) {}

            if (gatewayDialog.isApproved()) {
                log.info("Allowed {} to {}", request.getCertName(), prompt);
                if (gatewayDialog.isPersistent()) {
                    whiteList(request.getCertUsed());
                }
            } else {
                log.info("Denied {} to {}", request.getCertName(), prompt);
                if (gatewayDialog.isPersistent()) {
                    if (!request.hasCertificate()) {
                        anonymousItem.doClick(); // if always block anonymous requests -> flag menu item
                    } else {
                        blackList(request.getCertUsed());
                    }
                }
            }

            return gatewayDialog.isApproved();
        } else {
            return request.hasSavedCert();
        }
    }

    private void whiteList(Certificate cert) {
        if (FileUtilities.printLineToFile(Constants.ALLOW_FILE, cert.data())) {
            displayInfoMessage(String.format(Constants.ALLOW_SITES_TEXT, cert.getOrganization()));
        } else {
            displayErrorMessage("Failed to write to file (Insufficient user privileges)");
        }
    }

    private void blackList(Certificate cert) {
        if (FileUtilities.printLineToFile(Constants.BLOCK_FILE, cert.data())) {
            displayInfoMessage(String.format(Constants.BLOCK_SITES_TEXT, cert.getOrganization()));
        } else {
            displayErrorMessage("Failed to write to file (Insufficient user privileges)");
        }
    }

    public void setServer(Server server, int insecurePortIndex) {
        if (server != null && server.getConnectors().length > 0) {
            singleInstanceCheck(PrintSocketServer.INSECURE_PORTS, insecurePortIndex);

            displayInfoMessage("Server started on port(s) " + PrintSocketServer.getPorts(server));

            if (!headless) {
                aboutDialog.setServer(server);
                setDefaultIcon();
            }
        } else {
            displayErrorMessage("Invalid server");
        }
    }

    /**
     * Thread safe method for setting a fine status message.  Messages are suppressed unless "Show all
     * notifications" is checked.
     */
    public void displayInfoMessage(String text) {
        displayMessage(name, text, TrayIcon.MessageType.INFO);
    }

    /**
     * Thread safe method for setting the default icon
     */
    public void setDefaultIcon() {
        // Workaround for JDK-8252015
        if(SystemUtilities.isMac() && Constants.MASK_TRAY_SUPPORTED && !MacUtilities.jdkSupportsTemplateIcon()) {
            setIcon(DEFAULT_ICON, () -> MacUtilities.toggleTemplateIcon(tray.tray()));
        } else {
            setIcon(DEFAULT_ICON);
        }
    }

    /** Thread safe method for setting the error status message */
    public void displayErrorMessage(String text) {
        displayMessage(name, text, TrayIcon.MessageType.ERROR);
    }

    /** Thread safe method for setting the danger icon */
    public void setDangerIcon() {
        setIcon(DANGER_ICON);
    }

    /** Thread safe method for setting the warning status message */
    public void displayWarningMessage(String text) {
        displayMessage(name, text, TrayIcon.MessageType.WARNING);
    }

    /** Thread safe method for setting the warning icon */
    public void setWarningIcon() {
        setIcon(WARNING_ICON);
    }

    /** Thread safe method for setting the specified icon */
    private void setIcon(final IconCache.Icon i, Runnable whenDone) {
        if (tray != null && i != shownIcon) {
            shownIcon = i;
            refreshIcon(whenDone);
        }
    }

    private void setIcon(final IconCache.Icon i) {
        setIcon(i, null);
    }

    public void refreshIcon(final Runnable whenDone) {
        SwingUtilities.invokeLater(() -> {
            tray.setIcon(shownIcon);
            if(whenDone != null) {
                whenDone.run();
            }
        });
    }

    /**
     * Thread safe method for setting the specified status message
     *
     * @param caption The title of the tray message
     * @param text    The text body of the tray message
     * @param level   The message type: Level.INFO, .WARN, .SEVERE
     */
    private void displayMessage(final String caption, final String text, final TrayIcon.MessageType level) {
        if (!headless) {
            if (tray != null) {
                SwingUtilities.invokeLater(() -> {
                    boolean showAllNotifications = getPref(Constants.PREFS_NOTIFICATIONS, false);
                    if (showAllNotifications || level != TrayIcon.MessageType.INFO) {
                        tray.displayMessage(caption, text, level);
                    }
                });
            }
        } else {
            log.info("{}: [{}] {}", caption, level, text);
        }
    }

    public void singleInstanceCheck(java.util.List<Integer> insecurePorts, Integer insecurePortIndex) {
        for(int port : insecurePorts) {
            if (port != insecurePorts.get(insecurePortIndex)) {
                new SingleInstanceChecker(this, port);
            }
        }
    }

    public boolean isMonoclePreferred() {
        return getPref(Constants.PREFS_MONOCLE, true);
    }

    public boolean isHeadless() {
        return headless;
    }

    /**
     * Get boolean user pref: Searching "user", "app" and <code>System.getProperty(...)</code>.
     */
    private boolean getPref(String name, boolean defaultVal) {
        return "true".equalsIgnoreCase(PrefsSearch.get(prefs, App.getTrayProperties(), name, defaultVal + ""));
    }

    private void performIfIdle(int idleQualifier, ActionListener performer) {
        if (idleTimer != null) {
            idleTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    performer.actionPerformed(null);
                }
            }, idleQualifier);
        } else {
            log.warn("Idle actions have already been cleared due to activity, task not scheduled.");
        }
    }

    public void voidIdleActions() {
        if (idleTimer != null) {
            log.trace("Not idle, stopping any actions that haven't ran yet");
            idleTimer.cancel();
            idleTimer = null;
        }
    }

}
