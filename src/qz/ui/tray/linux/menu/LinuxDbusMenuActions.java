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

    public void setNotificationsEnabled(boolean enabled) {
        invokeOnEdt(() -> trayManager.setNotifications(enabled));
    }

    public boolean isMonocleEnabled() {
        return trayManager.isMonocleEnabled();
    }

    public void setMonocleEnabled(boolean enabled) {
        invokeOnEdt(() -> trayManager.setMonocle(enabled));
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

    public void setAnonymousRequestsBlocked(boolean blocked) {
        invokeOnEdt(() -> trayManager.setAnonymousRequestsBlocked(blocked));
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

    public void setAutoStartEnabled(boolean enabled) {
        invokeOnEdt(() -> trayManager.setAutoStart(enabled));
    }

    public void exit() {
        invokeOnEdt(trayManager::confirmAndExit);
    }

    private void invokeOnEdt(Runnable action) {
        SwingUtilities.invokeLater(action);
    }
}