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

        SystemUtilities.setSystemLookAndFeel();

        // Constructor iterates over all images denoted by IconCache.getTypes() and caches them
        iconCache = new IconCache();

        if (!headless) {
            try {
                tray = SystemTray.get();
                if (tray == null) {
                    throw new UnsupportedOperationException("Unable to attach tray for this OS");
                }
                tray.setImage(iconCache.getImage(IconCache.Icon.DANGER_ICON));
                tray.setTooltip(name);
            }
            catch(Exception e ) {
                log.error("Could not initialize tray, forcing headless mode", e);
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

            anonymousItem = new Checkbox("Block Anonymous Requests", anonymousListener);
            anonymousItem.setShortcut('k');
            anonymousItem.setChecked(Certificate.UNKNOWN.isBlocked());
            advancedMenu.add(anonymousItem);

            MenuItem logItem = new MenuItem("View Logs...", iconCache.getImage(IconCache.Icon.LOG_ICON), logListener);
            sitesItem.setShortcut('l');
            advancedMenu.add(logItem);

            advancedMenu.add(new Separator());

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

        aboutDialog = new AboutDialog(iconCache, name);
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
        tray.getMenu().add(startupItem);

        MenuItem exitItem = new MenuItem("Exit", iconCache.getImage(IconCache.Icon.EXIT_ICON), exitListener);
        exitItem.setShortcut('x');
        tray.getMenu().add(exitItem);
    }

    private final ActionListener openListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            try {
                dorkbox.util.Desktop.browseDirectory(shortcutCreator.getParentDirectory());
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

    private final ActionListener exitListener = e -> exit(0);

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
                    log.warn("Error removing " + toggleType + " entry");
                    checkBoxState = true;   // Set our checkbox back to true
                } else {
                    log.info("Successfully removed " + toggleType + " entry");
                }
            } else {
                checkBoxState = true;   // Set our checkbox back to true
            }
        } else {
            // Add shortcut entry
            if (!shortcutCreator.createShortcut(toggleType)) {
                log.warn("Error creating " + toggleType + " entry");
                checkBoxState = false;   // Set our checkbox back to false
            } else {
                log.info("Successfully added " + toggleType + " entry");
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
            log.error("Invalid certificate");
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
                            anonymousListener.actionPerformed(new ActionEvent(anonymousItem, ActionEvent.ACTION_PERFORMED, ""));
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
            log.info(String.format(Constants.WHITE_LIST, cert.getOrganization()));
        } else {
            log.error("Failed to write to file (Insufficient user privileges)");
        }
    }

    private void blackList(Certificate cert) {
        if (FileUtilities.printLineToFile(Constants.BLOCK_FILE, cert.data())) {
            log.info(String.format(Constants.BLACK_LIST, cert.getOrganization()));
        } else {
            log.error("Failed to write to file (Insufficient user privileges)");
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

            log.info("Server started on port(s) " + TrayManager.getPorts(server));

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
                    log.error("Error stopping print socket", e);
                }
            }));
        } else {
            log.error("Invalid server");
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
     * Thread safe method for setting the default icon
     */
    public void setDefaultIcon() {
        setIcon(IconCache.Icon.DEFAULT_ICON);
    }

    /** Thread safe method for setting the danger icon */
    public void setDangerIcon() {
        setIcon(IconCache.Icon.DANGER_ICON);
    }

    /** Thread safe method for setting the warning icon */
    public void setWarningIcon() {
        setIcon(IconCache.Icon.WARNING_ICON);
    }

    /** Thread safe method for setting the specified icon */
    private void setIcon(final IconCache.Icon i) {
        if (tray != null) {
            tray.setImage(iconCache.getImage(i, new Dimension(32,32)));
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
