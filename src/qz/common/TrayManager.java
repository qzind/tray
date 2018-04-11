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
import org.jdesktop.swinghelper.tray.JXTrayIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.auth.Certificate;
import qz.deploy.DeployUtilities;
import qz.deploy.LinuxCertificate;
import qz.deploy.WindowsDeploy;
import qz.ui.*;
import qz.ui.tray.ClassicTrayIcon;
import qz.ui.tray.ModernTrayIcon;
import qz.utils.*;
import qz.ws.PrintSocketServer;
import qz.ws.SingleInstanceChecker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static qz.common.Constants.USER_PREFS;
import static qz.common.I18NLoader.gettext;

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
    private JXTrayIcon tray;

    private ConfirmDialog confirmDialog;
    private GatewayDialog gatewayDialog;
    private AboutDialog aboutDialog;
    private LogDialog logDialog;
    private SiteManagerDialog sitesDialog;

    // Need a class reference to this so we can set it from the request dialog window
    private JCheckBoxMenuItem anonymousItem;

    // The name this UI component will use, i.e "QZ Print 1.9.0"
    private final String name;

    // The shortcut and startup helper
    private final DeployUtilities shortcutCreator;

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
        headless = isHeadless || USER_PREFS.getBoolean(Constants.PREFS_HEADLESS, false) || !SystemTray.isSupported();
        if (headless) {
            log.info("Running in headless mode");
        }

        // Setup the shortcut name so that the UI components can use it
        shortcutCreator = DeployUtilities.getSystemShortcutCreator();
        shortcutCreator.setShortcutName(Constants.ABOUT_TITLE);

        SystemUtilities.setSystemLookAndFeel();
        iconCache = new IconCache();

        if (!headless) {
            Image blank = new ImageIcon(new byte[1]).getImage();
            if (SystemUtilities.isWindows()) {
                tray = new JXTrayIcon(blank);
            } else if (SystemUtilities.isMac()) {
                tray = new ClassicTrayIcon(blank);
            } else {
                tray = new ModernTrayIcon(blank);
            }

            // Iterates over all images denoted by IconCache.getTypes() and caches them
            tray.setImage(iconCache.getImage(IconCache.Icon.DANGER_ICON, tray.getSize()));
            tray.setToolTip(name);

            try {
                SystemTray.getSystemTray().add(tray);
            }
            catch(AWTException awt) {
                log.error("Could not attach tray, forcing headless mode", awt);
                headless = true;
            }

            I18NLoader.addLocaleChangeListener(
                    (locale) -> initComponents()
            );
        }

        // Linux specific tasks
        if (SystemUtilities.isLinux()) {
            // Fix the tray icon to look proper on Ubuntu
            UbuntuUtilities.fixTrayIcons(iconCache);
            // Install cert into user's nssdb for Chrome, etc
            LinuxCertificate.installCertificate();
        } else if (SystemUtilities.isWindows()) {
            // Configure IE intranet zone via registry to allow websockets
            WindowsDeploy.configureIntranetZone();
            WindowsDeploy.configureEdgeLoopback();
        }

        initComponents();
    }

    private void initComponents() {

        if (!headless) {
            // The allow/block dialog
            gatewayDialog = new GatewayDialog(null, gettext("Action Required"), iconCache);

            // The ok/cancel dialog
            confirmDialog = new ConfirmDialog(null, gettext("Please Confirm"), iconCache);
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

        JMenu advancedMenu = new JMenu(gettext("Advanced"));
        advancedMenu.setMnemonic(KeyEvent.VK_A);
        advancedMenu.setIcon(iconCache.getIcon(IconCache.Icon.SETTINGS_ICON));

        JMenu localeMenu = new JMenu(gettext("Change Language"));
        localeMenu.setMnemonic(KeyEvent.VK_C);
        localeMenu.setIcon(iconCache.getIcon(IconCache.Icon.LANGUAGE_ICON));

        JMenuItem sitesItem = new JMenuItem(gettext("Site Manager..."), iconCache.getIcon(IconCache.Icon.SAVED_ICON));
        sitesItem.setMnemonic(KeyEvent.VK_M);
        sitesItem.addActionListener(savedListener);
        sitesDialog = new SiteManagerDialog(sitesItem, iconCache);

        anonymousItem = new JCheckBoxMenuItem(gettext("Block Anonymous Requests"));
        anonymousItem.setToolTipText(gettext("Blocks all requests that do no contain a valid certificate/signature"));
        anonymousItem.setMnemonic(KeyEvent.VK_K);
        anonymousItem.setState(Certificate.UNKNOWN.isBlocked());
        anonymousItem.addActionListener(anonymousListener);

        JMenuItem logItem = new JMenuItem(gettext("View Logs..."), iconCache.getIcon(IconCache.Icon.LOG_ICON));
        logItem.setMnemonic(KeyEvent.VK_L);
        logItem.addActionListener(logListener);
        logDialog = new LogDialog(logItem, iconCache);

        JCheckBoxMenuItem notificationsItem = new JCheckBoxMenuItem(gettext("Show all notifications"));
        notificationsItem.setToolTipText(gettext("Shows all connect/disconnect messages, useful for debugging purposes"));
        notificationsItem.setMnemonic(KeyEvent.VK_S);
        notificationsItem.setState(USER_PREFS.getBoolean(Constants.PREFS_NOTIFICATIONS, false));
        notificationsItem.addActionListener(notificationsListener);

        JMenuItem openItem = new JMenuItem(gettext("Open file location"), iconCache.getIcon(IconCache.Icon.FOLDER_ICON));
        openItem.setMnemonic(KeyEvent.VK_O);
        openItem.addActionListener(openListener);

        JMenuItem desktopItem = new JMenuItem(gettext("Create Desktop shortcut"), iconCache.getIcon(IconCache.Icon.DESKTOP_ICON));
        desktopItem.setMnemonic(KeyEvent.VK_D);
        desktopItem.addActionListener(desktopListener);

        advancedMenu.add(sitesItem);
        advancedMenu.add(anonymousItem);
        advancedMenu.add(logItem);
        advancedMenu.add(notificationsItem);
        advancedMenu.add(new JSeparator());
        advancedMenu.add(openItem);
        advancedMenu.add(desktopItem);

        I18NLoader.SUPPORTED_LOCALES.forEach(
            locale -> {
                JCheckBoxMenuItem localeMenuItem = new JCheckBoxMenuItem(locale.getDisplayName(locale));
                localeMenuItem.setState(Objects.equals(locale, I18NLoader.getCurrentLocale()));
                localeMenuItem.addActionListener((actionEvent) -> I18NLoader.changeLocale(locale));

                localeMenu.add(localeMenuItem);
            }
        );

        JMenuItem reloadItem = new JMenuItem(gettext("Reload"), iconCache.getIcon(IconCache.Icon.RELOAD_ICON));
        reloadItem.setMnemonic(KeyEvent.VK_R);
        reloadItem.addActionListener(reloadListener);

        JMenuItem aboutItem = new JMenuItem(gettext("About..."), iconCache.getIcon(IconCache.Icon.ABOUT_ICON));
        aboutItem.setMnemonic(KeyEvent.VK_B);
        aboutItem.addActionListener(aboutListener);
        aboutDialog = new AboutDialog(aboutItem, iconCache, name);
        aboutDialog.addPanelButton(sitesItem);
        aboutDialog.addPanelButton(logItem);
        aboutDialog.addPanelButton(openItem);

        if (SystemUtilities.isMac()) {
            MacUtilities.registerAboutDialog(aboutDialog);
            MacUtilities.registerQuitHandler(this);
        }

        JSeparator separator = new JSeparator();

        JCheckBoxMenuItem startupItem = new JCheckBoxMenuItem(gettext("Automatically start"));
        startupItem.setMnemonic(KeyEvent.VK_S);
        startupItem.setState(shortcutCreator.hasStartupShortcut());
        startupItem.addActionListener(startupListener);

        JMenuItem exitItem = new JMenuItem(gettext("Exit"), iconCache.getIcon(IconCache.Icon.EXIT_ICON));
        exitItem.addActionListener(exitListener);

        popup.add(advancedMenu);
        popup.add(localeMenu);
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
            USER_PREFS.setProperty(Constants.PREFS_NOTIFICATIONS, j.getState());
        }
    };

    private final ActionListener openListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            try {
                ShellUtilities.browseDirectory(shortcutCreator.getParentDirectory());
            }
            catch(Exception ex) {
                if (!SystemUtilities.isLinux() || !ShellUtilities.execute(new String[] {"xdg-open", shortcutCreator.getParentDirectory()})) {
                    showErrorDialog(String.format(gettext("Sorry, unable to open the file browser: %s"), ex.getLocalizedMessage()));
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
                showErrorDialog(gettext("Sorry, Reload has not yet been implemented."));
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
            boolean showAllNotifications = USER_PREFS.getBoolean(Constants.PREFS_NOTIFICATIONS, false);
            //: %s will be replaced by the name of the software in runtime
            if (!showAllNotifications || confirmDialog.prompt(String.format(gettext("Exit %s?"), name))) { exit(0); }
        }
    };

    public void exit(int returnCode) {
        USER_PREFS.save();
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
        if (e.getSource() instanceof JCheckBoxMenuItem) {
            checkBoxState = ((JCheckBoxMenuItem)e.getSource()).getState();
        }

        if (shortcutCreator.getJarPath() == null) {
            showErrorDialog(String.format(gettext("Unable to determine jar path: %s entry cannot succeed."), toggleType));
            return;
        }

        if (!checkBoxState) {
            // Remove shortcut entry
            if (confirmDialog.prompt(String.format(gettext("Remove %s from %s?"), name, toggleType))) {
                if (!shortcutCreator.removeShortcut(toggleType)) {
                    displayErrorMessage(String.format(gettext("Error removing %s entry"), toggleType));
                    checkBoxState = true;   // Set our checkbox back to true
                } else {
                    displayInfoMessage(String.format(gettext("Successfully removed %s entry"), toggleType));
                }
            } else {
                checkBoxState = true;   // Set our checkbox back to true
            }
        } else {
            // Add shortcut entry
            if (!shortcutCreator.createShortcut(toggleType)) {
                displayErrorMessage(String.format(gettext("Error creating %s entry"), toggleType));
                checkBoxState = false;   // Set our checkbox back to false
            } else {
                displayInfoMessage(String.format(gettext("Successfully added %s entry"), toggleType));
            }
        }

        if (e.getSource() instanceof JCheckBoxMenuItem) {
            ((JCheckBoxMenuItem)e.getSource()).setState(checkBoxState);
        }
    }

    /**
     * Displays a basic error dialog.
     */
    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(null, message, name, JOptionPane.ERROR_MESSAGE);
    }

    public boolean showGatewayDialog(final Certificate cert, final String prompt, final Point position) {
        if (cert == null) {
            displayErrorMessage(gettext("Invalid certificate"));
            return false;
        } else {
            if (!headless) {
                try {
                    SwingUtilities.invokeAndWait(() -> gatewayDialog.prompt(String.format(gettext("%%s wants to %s"), prompt), cert, position));
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
                            anonymousItem.doClick(); // if always block anonymous requests -> flag menu item
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
            displayInfoMessage(String.format(Constants.WHITE_LIST.get(), cert.getOrganization()));
        } else {
            displayErrorMessage(gettext("Failed to write to file (Insufficient user privileges)"));
        }
    }

    private void blackList(Certificate cert) {
        if (FileUtilities.printLineToFile(Constants.BLOCK_FILE, cert.data())) {
            displayInfoMessage(String.format(Constants.BLACK_LIST.get(), cert.getOrganization()));
        } else {
            displayErrorMessage(gettext("Failed to write to file (Insufficient user privileges)"));
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

            displayInfoMessage(String.format(gettext("Server started on port(s) %s"), TrayManager.getPorts(server)));

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
                    displayErrorMessage(String.format(gettext("Error stopping print socket: %s"), e.getLocalizedMessage()));
                }
            }));
        } else {
            displayErrorMessage(gettext("Invalid server"));
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
            SwingUtilities.invokeLater(() -> tray.setImage(iconCache.getImage(i, tray.getSize())));
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
                    boolean showAllNotifications = USER_PREFS.getBoolean(Constants.PREFS_NOTIFICATIONS, false);
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
