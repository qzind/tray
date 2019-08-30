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

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.auth.Certificate;
import qz.auth.RequestState;
import qz.deploy.DeployUtilities;
import qz.deploy.LinuxCertificate;
import qz.deploy.WindowsDeploy;
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
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the icons and actions associated with the TrayIcon
 *
 * @author Tres Finocchiaro
 */
public class TrayManager {

    private static final Logger log = LoggerFactory.getLogger(TrayManager.class);

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
    private final DeployUtilities shortcutCreator;

    private final PropertyHelper prefs;

    // Action to run when reload is triggered
    private Thread reloadThread;

    public TrayManager() {
        this(false);
    }

    /**
     * Create a AutoHideJSystemTray with the specified name/text
     */
    public TrayManager(boolean isHeadless) {
        name = Constants.ABOUT_TITLE + " " + Constants.VERSION;

        prefs = new PropertyHelper(SystemUtilities.getDataDirectory() + File.separator + Constants.PREFS_FILE + ".properties");

        //headless if turned on by user or unsupported by environment
        headless = isHeadless || prefs.getBoolean(Constants.PREFS_HEADLESS, false) || GraphicsEnvironment.isHeadless();
        if (headless) {
            log.info("Running in headless mode");
        }

        // Setup the shortcut name so that the UI components can use it
        shortcutCreator = DeployUtilities.getSystemShortcutCreator();
        shortcutCreator.setShortcutName(Constants.ABOUT_TITLE);

        SystemUtilities.setSystemLookAndFeel();
        iconCache = new IconCache();

        if (!headless && SystemTray.isSupported()) { // UI mode with tray
            if (SystemUtilities.isWindows()) {
                tray = TrayType.JX.init();
                // Undocumented HiDPI behavior
                tray.setImageAutoSize(true);
            } else if (SystemUtilities.isMac()) {
                tray = TrayType.CLASSIC.init();
            } else {
                tray = TrayType.MODERN.init();
            }

            // OS-specific tray icon handling
            if (SystemTray.isSupported()) {
                iconCache.fixTrayIcons(SystemUtilities.isDarkMode());
            }

            // Iterates over all images denoted by IconCache.getTypes() and caches them
            tray.setImage(iconCache.getImage(IconCache.Icon.DANGER_ICON, tray.getSize()));
            tray.setToolTip(name);

            try {
                SystemTray.getSystemTray().add(tray.tray());
            }
            catch(AWTException awt) {
                log.error("Could not attach tray, forcing headless mode", awt);
                headless = true;
            }
        } else if (!headless) { // UI mode without tray
            tray = TrayType.TASKBAR.init(exitListener);
            tray.setImage(iconCache.getImage(IconCache.Icon.DANGER_ICON, tray.getSize()));
            tray.setToolTip(name);
            tray.showTaskbar();
        }

        // TODO: Remove when fixed upstream.  See issue #393
        if (SystemUtilities.isUnix() && !isHeadless) {
            // Update printer list in CUPS immediately (normally 2min)
            System.setProperty("sun.java2d.print.polling", "false");
        }
        if (SystemUtilities.isLinux()) {
            // Install cert into user's nssdb for Chrome, etc
            LinuxCertificate.installCertificate();
        } else if (SystemUtilities.isWindows()) {
            // Configure IE intranet zone via registry to allow websockets
            WindowsDeploy.configureIntranetZone();
            WindowsDeploy.configureEdgeLoopback();
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
                boolean darkMode = SystemUtilities.isDarkMode();
                while(true) {
                    try {
                        Thread.sleep(1000);
                        if (darkMode != SystemUtilities.isDarkMode(true)) {
                            darkMode = SystemUtilities.isDarkMode();
                            iconCache.fixTrayIcons(darkMode);
                            refreshIcon();
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
                    } catch(InterruptedException ignore) {}
                }
            }).start();
        }

        if (tray != null) {
            addMenuItems();
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
        advancedMenu.setIcon(iconCache.getIcon(IconCache.Icon.SETTINGS_ICON));

        JMenuItem sitesItem = new JMenuItem("Site Manager...", iconCache.getIcon(IconCache.Icon.SAVED_ICON));
        sitesItem.setMnemonic(KeyEvent.VK_M);
        sitesItem.addActionListener(savedListener);
        sitesDialog = new SiteManagerDialog(sitesItem, iconCache);
        componentList.add(sitesDialog);

        anonymousItem = new JCheckBoxMenuItem("Block Anonymous Requests");
        anonymousItem.setToolTipText("Blocks all requests that do no contain a valid certificate/signature");
        anonymousItem.setMnemonic(KeyEvent.VK_K);
        anonymousItem.setState(Certificate.UNKNOWN.isBlocked());
        anonymousItem.addActionListener(anonymousListener);

        JMenuItem logItem = new JMenuItem("View Logs...", iconCache.getIcon(IconCache.Icon.LOG_ICON));
        logItem.setMnemonic(KeyEvent.VK_L);
        logItem.addActionListener(logListener);
        logDialog = new LogDialog(logItem, iconCache);
        componentList.add(logDialog);

        JCheckBoxMenuItem notificationsItem = new JCheckBoxMenuItem("Show all notifications");
        notificationsItem.setToolTipText("Shows all connect/disconnect messages, useful for debugging purposes");
        notificationsItem.setMnemonic(KeyEvent.VK_S);
        notificationsItem.setState(prefs.getBoolean(Constants.PREFS_NOTIFICATIONS, false));
        notificationsItem.addActionListener(notificationsListener);

        JMenuItem openItem = new JMenuItem("Open file location", iconCache.getIcon(IconCache.Icon.FOLDER_ICON));
        openItem.setMnemonic(KeyEvent.VK_O);
        openItem.addActionListener(openListener);

        JMenuItem desktopItem = new JMenuItem("Create Desktop shortcut", iconCache.getIcon(IconCache.Icon.DESKTOP_ICON));
        desktopItem.setMnemonic(KeyEvent.VK_D);
        desktopItem.addActionListener(desktopListener());

        advancedMenu.add(sitesItem);
        advancedMenu.add(anonymousItem);
        advancedMenu.add(logItem);
        advancedMenu.add(notificationsItem);
        advancedMenu.add(new JSeparator());
        advancedMenu.add(openItem);
        advancedMenu.add(desktopItem);


        JMenuItem reloadItem = new JMenuItem("Reload", iconCache.getIcon(IconCache.Icon.RELOAD_ICON));
        reloadItem.setMnemonic(KeyEvent.VK_R);
        reloadItem.addActionListener(reloadListener);

        JMenuItem aboutItem = new JMenuItem("About...", iconCache.getIcon(IconCache.Icon.ABOUT_ICON));
        aboutItem.setMnemonic(KeyEvent.VK_B);
        aboutItem.addActionListener(aboutListener);
        aboutDialog = new AboutDialog(aboutItem, iconCache, name);
        aboutDialog.addPanelButton(sitesItem);
        aboutDialog.addPanelButton(logItem);
        aboutDialog.addPanelButton(openItem);
        componentList.add(aboutDialog);

        if (SystemUtilities.isMac()) {
            MacUtilities.registerAboutDialog(aboutDialog);
            MacUtilities.registerQuitHandler(this);
        }

        JSeparator separator = new JSeparator();

        JCheckBoxMenuItem startupItem = new JCheckBoxMenuItem("Automatically start");
        startupItem.setMnemonic(KeyEvent.VK_S);
        startupItem.setState(shortcutCreator.isAutostart());
        startupItem.addActionListener(startupListener());
        if (!shortcutCreator.canAutoStart()) {
            startupItem.setEnabled(false);
            startupItem.setState(false);
            startupItem.setToolTipText("Autostart has been disabled by the administrator");
        }

        JMenuItem exitItem = new JMenuItem("Exit", iconCache.getIcon(IconCache.Icon.EXIT_ICON));
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
            JCheckBoxMenuItem j = (JCheckBoxMenuItem)e.getSource();
            prefs.setProperty(Constants.PREFS_NOTIFICATIONS, j.getState());
        }
    };

    private final ActionListener openListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            try {
                ShellUtilities.browseDirectory(shortcutCreator.getParentDirectory());
            }
            catch(Exception ex) {
                if (!SystemUtilities.isLinux() || !ShellUtilities.execute(new String[] {"xdg-open", shortcutCreator.getParentDirectory()})) {
                    showErrorDialog("Sorry, unable to open the file browser: " + ex.getLocalizedMessage());
                }
            }
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
            FileUtilities.deleteFromFile(Constants.BLOCK_FILE, Certificate.UNKNOWN.data());
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
            if (shortcutCreator.setAutostart(source.getState())) {
                displayInfoMessage("Successfully " + (source.getState() ? "enabled" : "disabled") + " autostart");
            } else {
                displayErrorMessage("Error " + (source.getState() ? "enabling" : "disabling") + " autostart");
            }
            source.setState(shortcutCreator.isAutostart());
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
            boolean showAllNotifications = prefs.getBoolean(Constants.PREFS_NOTIFICATIONS, false);
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
            displayInfoMessage(String.format(Constants.WHITE_LIST, cert.getOrganization()));
        } else {
            displayErrorMessage("Failed to write to file (Insufficient user privileges)");
        }
    }

    private void blackList(Certificate cert) {
        if (FileUtilities.printLineToFile(Constants.BLOCK_FILE, cert.data())) {
            displayInfoMessage(String.format(Constants.BLACK_LIST, cert.getOrganization()));
        } else {
            displayErrorMessage("Failed to write to file (Insufficient user privileges)");
        }
    }

    /**
     * Sets the WebSocket Server instance for displaying port information and restarting the server
     *
     * @param server            The Server instance contain to bind the reload action to
     * @param running           Object used to notify PrintSocket to reiterate its main while loop
     * @param securePortIndex   Object used to notify PrintSocket to reset its port array counter
     * @param insecurePortIndex Object used to notify PrintSocket to reset its port array counter
     */
    public void setServer(final Server server, final AtomicBoolean running, final AtomicInteger securePortIndex, final AtomicInteger insecurePortIndex) {
        if (server != null && server.getConnectors().length > 0) {
            singleInstanceCheck(PrintSocketServer.INSECURE_PORTS, insecurePortIndex.get());

            displayInfoMessage("Server started on port(s) " + TrayManager.getPorts(server));

            if (!headless) {
                aboutDialog.setServer(server);
                setDefaultIcon();
            }

            setReloadThread(new Thread(() -> {
                try {
                    setDangerIcon();
                    running.set(false);
                    securePortIndex.set(0);
                    insecurePortIndex.set(0);

                    server.stop();
                }
                catch(Exception e) {
                    displayErrorMessage("Error stopping print socket: " + e.getLocalizedMessage());
                }
            }));
        } else {
            displayErrorMessage("Invalid server");
        }
    }

    /**
     * Returns a String representation of the ports assigned to the specified Server
     */
    public static String getPorts(Server server) {
        StringBuilder ports = new StringBuilder();
        for(Connector c : server.getConnectors()) {
            if (ports.length() > 0) {
                ports.append(", ");
            }

            ports.append(((ServerConnector)c).getLocalPort());
        }

        return ports.toString();
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
        setIcon(IconCache.Icon.DEFAULT_ICON);
    }

    /** Thread safe method for setting the error status message */
    public void displayErrorMessage(String text) {
        displayMessage(name, text, TrayIcon.MessageType.ERROR);
    }

    /** Thread safe method for setting the danger icon */
    public void setDangerIcon() {
        setIcon(IconCache.Icon.DANGER_ICON);
    }

    /** Thread safe method for setting the warning status message */
    public void displayWarningMessage(String text) {
        displayMessage(name, text, TrayIcon.MessageType.WARNING);
    }

    /** Thread safe method for setting the warning icon */
    public void setWarningIcon() {
        setIcon(IconCache.Icon.WARNING_ICON);
    }

    /** Thread safe method for setting the specified icon */
    private void setIcon(final IconCache.Icon i) {
        if (tray != null && i != shownIcon) {
            shownIcon = i;
            refreshIcon();
        }
    }

    public void refreshIcon() {
        SwingUtilities.invokeLater(() -> tray.setImage(iconCache.getImage(shownIcon, tray.getSize())));
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
                    boolean showAllNotifications = prefs.getBoolean(Constants.PREFS_NOTIFICATIONS, false);
                    if (showAllNotifications || level == TrayIcon.MessageType.ERROR) {
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

}
