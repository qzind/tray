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
import qz.deploy.DeployUtilities;
import qz.deploy.LinuxCertificate;
import qz.deploy.WindowsDeploy;
import qz.ui.*;
import qz.utils.*;
import qz.ws.PrintSocketServer;
import qz.ws.SingleInstanceChecker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import dorkbox.systemTray.Checkbox;
import dorkbox.systemTray.Menu;
import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.Separator;
import dorkbox.systemTray.SystemTray;

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
    private SystemTray tray;

    private ConfirmDialog confirmDialog;
    private GatewayDialog gatewayDialog;
    private AboutDialog aboutDialog;
    private LogDialog logDialog;
    private SiteManagerDialog sitesDialog;

    // Need a class reference to this so we can set it from the request dialog window
    private Checkbox anonymousItem;

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

        headless = isHeadless || prefs.getBoolean(Constants.PREFS_HEADLESS, false);
        if (headless) {
            log.info("Running in headless mode");
        }

        // Setup the shortcut name so that the UI components can use it
        shortcutCreator = DeployUtilities.getSystemShortcutCreator();
        shortcutCreator.setShortcutName(Constants.ABOUT_TITLE);

        // TODO: DON'T FORGET TO ADDRESS THIS. It causes a gtk2 vs 3 error on elementaryOS
        // This at least mitigates the problem, still need to fix
        if (!dorkbox.util.OS.isLinux() || dorkbox.systemTray.jna.linux.Gtk.isGtk2) {
            SystemUtilities.setSystemLookAndFeel();
        }

        // Constructor iterates over all images denoted by IconCache.getTypes() and caches them
        iconCache = new IconCache();

        if (!headless) {
            try {
                tray = SystemTray.getNative();
                tray.setImage(iconCache.getImage(IconCache.Icon.DANGER_ICON));
                // tray.setToolTip(name); Not supported by Dorkbox (yet)
            }
            catch(Exception e ) {
                log.error("Could not initialize tray, forcing headless mode", e);
                headless = true;
            }
            if (!headless && (tray == null)) {
                log.error("Unknown tray init error, most likely unsupported os, Forcing headless mode");
                headless = true;
            }
        }

        // Linux specific tasks
        if (SystemUtilities.isLinux()) {
            // Install cert into user's nssdb for Chrome, etc
            LinuxCertificate.installCertificate();
        } else if (SystemUtilities.isWindows()) {
            // Configure IE intranet zone via registry to allow websockets
            WindowsDeploy.configureIntranetZone();
            WindowsDeploy.configureEdgeLoopback();
        }

        if (!headless) {
            // The allow/block dialog
            gatewayDialog = new GatewayDialog(null, "Action Required", iconCache);

            // The ok/cancel dialog
            confirmDialog = new ConfirmDialog(null, "Please Confirm", iconCache);
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
        Menu advancedMenu = new Menu("Advanced", iconCache.getImage(IconCache.Icon.SETTINGS_ICON));
        advancedMenu.setShortcut('a');
        tray.getMenu().add(advancedMenu);
        {
            MenuItem sitesItem = new MenuItem("Site Manager...", iconCache.getImage(IconCache.Icon.SAVED_ICON), savedListener);
            sitesItem.setShortcut('m');
            advancedMenu.add(sitesItem);

            Checkbox anonymousItem = new Checkbox("Block Anonymous Requests", anonymousListener);
            anonymousItem.setShortcut('k');
            anonymousItem.setChecked(Certificate.UNKNOWN.isBlocked());
            //anonymousItem.setToolTipText("Blocks all requests that do no contain a valid certificate/signature");
            advancedMenu.add(anonymousItem);

            MenuItem logItem = new MenuItem("View Logs...", iconCache.getImage(IconCache.Icon.LOG_ICON), logListener);
            sitesItem.setShortcut('l');
            advancedMenu.add(logItem);

            Checkbox notificationsItem = new Checkbox("Show all notifications", notificationsListener);
            notificationsItem.setShortcut('s');
            notificationsItem.setChecked(prefs.getBoolean(Constants.PREFS_NOTIFICATIONS, false));
            //anonymousItem.setToolTipText("Blocks all requests that do no contain a valid certificate/signature");
            advancedMenu.add(notificationsItem);

            MenuItem openItem = new MenuItem("Open file location", iconCache.getImage(IconCache.Icon.FOLDER_ICON), openListener);
            sitesItem.setShortcut('o');
            advancedMenu.add(openItem);

            MenuItem desktopItem = new MenuItem("Create Desktop shortcut", iconCache.getImage(IconCache.Icon.DESKTOP_ICON), desktopListener);
            sitesItem.setShortcut('d');
            advancedMenu.add(desktopItem);
        }

        MenuItem reloadItem = new MenuItem("Reload", iconCache.getImage(IconCache.Icon.RELOAD_ICON), reloadListener);
        reloadItem.setShortcut('r');
        tray.getMenu().add(reloadItem);

        MenuItem aboutItem = new MenuItem("About...", iconCache.getImage(IconCache.Icon.ABOUT_ICON), aboutListener);
        aboutItem.setShortcut('b');
        tray.getMenu().add(aboutItem);

        aboutDialog = new AboutDialog(null, iconCache, name);
        {
            JMenuItem siteButton = new JMenuItem("Site Manager...", iconCache.getIcon(IconCache.Icon.SAVED_ICON));
            siteButton.setMnemonic(KeyEvent.VK_M);
            siteButton.addActionListener(savedListener);
            sitesDialog = new SiteManagerDialog(siteButton, iconCache);

            JMenuItem logButton = new JMenuItem("View Logs...", iconCache.getIcon(IconCache.Icon.LOG_ICON));
            logButton.setMnemonic(KeyEvent.VK_L);
            logButton.addActionListener(logListener);
            logDialog = new LogDialog(logButton, iconCache);

            JMenuItem openButton = new JMenuItem("Open file location", iconCache.getIcon(IconCache.Icon.FOLDER_ICON));
            openButton.setMnemonic(KeyEvent.VK_O);
            openButton.addActionListener(openListener);

            aboutDialog.addPanelButton(siteButton);
            aboutDialog.addPanelButton(logButton);
            aboutDialog.addPanelButton(openButton);
        }
        tray.getMenu().add(new Separator());

        Checkbox startupItem = new Checkbox("Automatically start", startupListener);
        startupItem.setShortcut('s');
        startupItem.setChecked(shortcutCreator.hasStartupShortcut());
        //anonymousItem.setToolTipText("Blocks all requests that do no contain a valid certificate/signature");
        tray.getMenu().add(startupItem);

        MenuItem exitItem = new MenuItem("Exit", iconCache.getImage(IconCache.Icon.EXIT_ICON), exitListener);
        exitItem.setShortcut('x');
        tray.getMenu().add(exitItem);
    }


    private final ActionListener notificationsListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            Checkbox j = (Checkbox)e.getSource();
            prefs.setProperty(Constants.PREFS_NOTIFICATIONS, j.getChecked());
        }
    };

    private final ActionListener openListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            try {
                boolean avoidGTK2 = dorkbox.util.OS.isLinux() && !dorkbox.systemTray.jna.linux.Gtk.isGtk2;
                ShellUtilities.browseDirectory(shortcutCreator.getParentDirectory(), avoidGTK2);
            }
            catch(Exception ex) {
                if (!SystemUtilities.isLinux() || !ShellUtilities.execute(new String[] {"xdg-open", shortcutCreator.getParentDirectory()})) {
                    showErrorDialog("Sorry, unable to open the file browser: " + ex.getLocalizedMessage());
                }
            }
        }
    };

    private final ActionListener desktopListener = e -> shortcutToggle(e, DeployUtilities.ToggleType.DESKTOP);

    private final ActionListener savedListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            sitesDialog.setVisible(true);
        }
    };

    private final ActionListener anonymousListener = e -> {
        boolean checkBoxState = true;
        if (e.getSource() instanceof Checkbox) {
            checkBoxState = ((Checkbox)e.getSource()).getChecked();
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

    private final ActionListener startupListener = e -> shortcutToggle(e, DeployUtilities.ToggleType.STARTUP);

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
     * Process toggle/checkbox events as they relate to creating shortcuts
     *
     * @param e          The ActionEvent passed in from an ActionListener
     * @param toggleType Either ShortcutUtilities.TOGGLE_TYPE_STARTUP or
     *                   ShortcutUtilities.TOGGLE_TYPE_DESKTOP
     */
    private void shortcutToggle(ActionEvent e, DeployUtilities.ToggleType toggleType) {
        // Assume true in case its a regular JMenuItem
        boolean checkBoxState = true;
        if (e.getSource() instanceof Checkbox) {
            checkBoxState = ((Checkbox)e.getSource()).getChecked();
        }

        if (shortcutCreator.getJarPath() == null) {
            showErrorDialog("Unable to determine jar path; " + toggleType + " entry cannot succeed.");
            return;
        }

        if (!checkBoxState) {
            // Remove shortcut entry
            if (confirmDialog.prompt("Remove " + name + " from " + toggleType + "?")) {
                if (!shortcutCreator.removeShortcut(toggleType)) {
                    displayErrorMessage("Error removing " + toggleType + " entry");
                    checkBoxState = true;   // Set our checkbox back to true
                } else {
                    displayInfoMessage("Successfully removed " + toggleType + " entry");
                }
            } else {
                checkBoxState = true;   // Set our checkbox back to true
            }
        } else {
            // Add shortcut entry
            if (!shortcutCreator.createShortcut(toggleType)) {
                displayErrorMessage("Error creating " + toggleType + " entry");
                checkBoxState = false;   // Set our checkbox back to false
            } else {
                displayInfoMessage("Successfully added " + toggleType + " entry");
            }
        }

        if (e.getSource() instanceof Checkbox) {
            ((Checkbox)e.getSource()).setChecked(checkBoxState);
        }
    }

    /**
     * Displays a basic error dialog.
     */
    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(null, message, name, JOptionPane.ERROR_MESSAGE);
    }

    public boolean showGatewayDialog(final Certificate cert, final String prompt) {
        if (cert == null) {
            displayErrorMessage("Invalid certificate");
            return false;
        } else {
            if (!headless) {
                try {
                    SwingUtilities.invokeAndWait(() -> gatewayDialog.prompt("%s wants to " + prompt, cert));
                }
                catch(Exception ignore) {}

                if (gatewayDialog.isApproved()) {
                    log.info("Allowed {} to {}", cert.getCommonName(), prompt);
                    if (gatewayDialog.isPersistent()) {
                        whiteList(cert);
                    }
                } else {
                    log.info("Denied {} to {}", cert.getCommonName(), prompt);
                    if (gatewayDialog.isPersistent()) {
                        if (Certificate.UNKNOWN.equals(cert)) {
                            anonymousItem.setChecked(true); // if always block anonymous requests -> flag menu item
                        } else {
                            blackList(cert);
                        }
                    }
                }

                return gatewayDialog.isApproved();
            } else {
                return cert.isTrusted() && cert.isSaved();
            }
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
        if (tray != null) {
            // Gross, if you know a better way, feel free to change this
            Image blank = new ImageIcon(new byte[1]).getImage();
            Dimension size = new TrayIcon(blank).getSize();
            tray.setImage(iconCache.getImage(i, size));
        }
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
                        // TODO: add notification support to dorkbox.systemtray
                        // tray.displayMessage(caption, text, level);
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
