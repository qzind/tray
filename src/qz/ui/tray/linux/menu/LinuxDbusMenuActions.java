package qz.ui.tray.linux.menu;

import qz.common.TrayManager;
import qz.utils.FileUtilities;
import qz.utils.ShellUtilities;

import javax.swing.*;

public class LinuxDbusMenuActions {

    private final TrayManager trayManager;

    public LinuxDbusMenuActions(TrayManager trayManager) {
        this.trayManager = trayManager;
    }

    public void browseAppDirectory() {
        invokeOnEdt(ShellUtilities::browseAppDirectory);
    }

    public void browseUserDirectory() {
        invokeOnEdt(() -> ShellUtilities.browseDirectory(FileUtilities.USER_DIR));
    }

    public void browseSharedDirectory() {
        invokeOnEdt(() -> ShellUtilities.browseDirectory(FileUtilities.SHARED_DIR));
    }

    public boolean areNotificationsEnabled() {
        return trayManager.areNotificationsEnabled();
    }

    public void setNotificationsEnabled(boolean enabled, Runnable completion) {
        invokeOnEdt(() -> trayManager.setNotifications(enabled), completion);
    }

    public boolean isMonocleEnabled() {
        return trayManager.isMonocleEnabled();
    }

    public void setMonocleEnabled(boolean enabled, Runnable completion) {
        invokeOnEdt(() -> trayManager.setMonocle(enabled), completion);
    }

    public void showLogs() {
        invokeOnEdt(trayManager::showLogs);
    }

    public void zipLogs() {
        invokeOnEdt(FileUtilities::zipLogs);
    }

    public void showSiteManager() {
        invokeOnEdt(trayManager::showSiteManager);
    }

    public void createDesktopShortcut() {
        invokeOnEdt(trayManager::createDesktopShortcut);
    }

    public boolean areAnonymousRequestsBlocked() {
        return trayManager.areAnonymousRequestsBlocked();
    }

    public void setAnonymousRequestsBlocked(boolean blocked, Runnable completion) {
        invokeOnEdt(() -> trayManager.setAnonymousRequestsBlocked(blocked), completion);
    }

    public void reload() {
        invokeOnEdt(trayManager::reload);
    }

    public void showAbout() {
        invokeOnEdt(trayManager::showAbout);
    }

    public boolean canAutoStart() {
        return trayManager.canAutoStart();
    }

    public boolean isAutoStartEnabled() {
        return trayManager.isAutoStartEnabled();
    }

    public void setAutoStartEnabled(boolean enabled, Runnable completion) {
        invokeOnEdt(() -> trayManager.setAutoStart(enabled), completion);
    }

    public void exit() {
        invokeOnEdt(trayManager::confirmAndExit);
    }

    private void invokeOnEdt(Runnable action) {
        SwingUtilities.invokeLater(action);
    }

    /**
     * Runs a state-changing tray action on Swing's event dispatch thread and
     * invokes its completion callback only after the action has finished
     *
     * This lets DBusMenu read and publish the authoritative resulting state
     * instead of assuming the requested checkbox value was applied, since
     * actions such as changing autostart can be canceled or fail
     *
     * @param action the Swing-side action which may change tray state
     * @param completion the callback which publishes the resulting state
     */
    private void invokeOnEdt(Runnable action, Runnable completion) {
        SwingUtilities.invokeLater(() -> {
            try {
                // TrayManager and its dialogs must run on Swing's EDT
                action.run();
            }
            finally {
                // Always republish the actual state even when the action fails
                completion.run();
            }
        });
    }
}
